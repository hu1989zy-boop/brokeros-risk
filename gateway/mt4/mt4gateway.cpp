// mt4gateway.cpp — MT4 -> ADR-024 canonical -> Phase A envelope -> Kafka (Windows line, W2).
//
// Boundary (AGENTS.md L75 + ADR-024 + gateway Implementation Design V2):
//   - Read/ingest ONLY. Never calls any MT4 write/trade/admin operation.
//   - No MT4 native type crosses the Kafka boundary; only ADR-024 canonical JSON.
//   - No secret/PII in code or logs. Credentials come in as argv (dev) or should be
//     sourced from secure config in production.
//
// Scope of THIS file (W1+W2, hand-built on the Windows line):
//   snapshot pull (TradesRequest) -> canonical TRADE_ACTIVITY(STATE) -> Phase A envelope
//   -> produce to Kafka `trading-data.canonical` (key = accountRef), idempotent producer.
//
// Known deferred gaps (tracked in Q-015 §17 / review conditions — NOT yet implemented here):
//   - per-account sourceSequence (currently a single global counter);
//   - reconnect epoch field in the envelope;
//   - live pumping (PumpingSwitchEx) instead of a one-shot snapshot;
//   - account `currency` on the canonical event (ADR-024 §3 money rule).
// See gateway/mt4/README.md "Known gaps / not yet done".

#ifndef _WIN64
#error Build as 64-bit ("x64 Native Tools Command Prompt for VS").
#endif
#include <winsock2.h>
#include <windows.h>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <ctime>
#include <string>
#include "MT4ManagerAPI.h"
#include <librdkafka/rdkafka.h>

static const char* SideName(int cmd){ switch(cmd){ case OP_BUY:return"BUY"; case OP_SELL:return"SELL"; case OP_BUY_LIMIT:return"BUY_LIMIT"; case OP_SELL_LIMIT:return"SELL_LIMIT"; case OP_BUY_STOP:return"BUY_STOP"; case OP_SELL_STOP:return"SELL_STOP"; case OP_BALANCE:return"BALANCE"; case OP_CREDIT:return"CREDIT"; default:return"OTHER"; } }
static const char* ReasonName(int r){ switch(r){ case TR_REASON_CLIENT:return"CLIENT"; case TR_REASON_EXPERT:return"EXPERT"; case TR_REASON_DEALER:return"DEALER"; case TR_REASON_SIGNAL:return"SIGNAL"; case TR_REASON_GATEWAY:return"GATEWAY"; case TR_REASON_MOBILE:return"MOBILE"; case TR_REASON_WEB:return"WEB"; case TR_REASON_API:return"API"; default:return"OTHER"; } }
static std::string jsonSafe(const char* s){ std::string o; for(; s && *s; ++s){ unsigned char c=(unsigned char)*s; if(c=='"'||c=='\\'){ o+='\\'; o+=(char)c; } else if(c>=0x20 && c<0x7f){ o+=(char)c; } } return o; }
static std::string toUtcIso(long t,long offset){ time_t utc=(time_t)(t-offset); struct tm g; gmtime_s(&g,&utc); char b[32]; strftime(b,sizeof(b),"%Y-%m-%dT%H:%M:%SZ",&g); return b; }

static long g_delivered=0, g_failed=0;
static void dr_cb(rd_kafka_t* rk,const rd_kafka_message_t* m,void* o){ if(m->err){ g_failed++; fprintf(stderr,"DELIVERY FAILED: %s\n",rd_kafka_err2str(m->err)); } else g_delivered++; }

int main(int argc,char** argv){
  if(argc<6){ printf("Usage: mt4gateway.exe <server:port> <login> <password> <sourceServerId> <kafka_brokers> [maxTrades]\n"); return 1; }
  const char* server=argv[1]; int login=atoi(argv[2]); const char* password=argv[3];
  const char* srvId=argv[4]; const char* brokers=argv[5]; int maxTrades=(argc>=7)?atoi(argv[6]):50;
  const char* topic="trading-data.canonical";

  char err[512]; rd_kafka_conf_t* conf=rd_kafka_conf_new();
  rd_kafka_conf_set(conf,"bootstrap.servers",brokers,err,sizeof(err));
  rd_kafka_conf_set(conf,"broker.address.family","v4",err,sizeof(err)); // force IPv4 (localhost->127.0.0.1)
  rd_kafka_conf_set(conf,"enable.idempotence","true",err,sizeof(err));
  rd_kafka_conf_set_dr_msg_cb(conf,dr_cb);
  rd_kafka_t* rk=rd_kafka_new(RD_KAFKA_PRODUCER,conf,err,sizeof(err));
  if(!rk){ printf("ERROR: kafka producer create failed: %s\n",err); return 2; }

  CManagerFactory factory;
  if(!factory.IsValid()){ printf("ERROR: mtmanapi64.dll not loaded\n"); return 3; }
  factory.WinsockStartup();
  CManagerInterface* man=factory.Create(ManAPIVersion);
  if(!man){ printf("ERROR: Create failed\n"); return 4; }
  if(man->Connect(server)!=RET_OK){ printf("ERROR: Connect failed\n"); return 5; }
  if(man->Login(login,password)!=RET_OK){ printf("ERROR: Login failed\n"); return 6; }
  long offset=(long)man->ServerTime()-(long)time(NULL);
  printf("Login OK. serverUtcOffsetSeconds=%ld. Producing to %s / %s ...\n",offset,brokers,topic);

  int total=0; TradeRecord* tr=man->TradesRequest(&total);
  int n=(total<maxTrades)?total:maxTrades; long seq=0;
  for(int i=0;i<n && tr;i++){ TradeRecord& t=tr[i]; char acct[32]; sprintf(acct,"%d",t.login);
    std::string occurredAt=toUtcIso((long)t.timestamp,offset); std::string openUtc=toUtcIso((long)t.open_time,offset);
    std::string comment=jsonSafe(t.comment); std::string symbol=jsonSafe(t.symbol); char payload[2048];
    snprintf(payload,sizeof(payload),"{\"canonicalVersion\":1,\"platform\":\"MT4\",\"sourceServerId\":\"%s\",\"eventKind\":\"TRADE_ACTIVITY\",\"lifecycleHint\":\"STATE\",\"accountRef\":\"%d\",\"orderRef\":\"%d\",\"positionRef\":\"%d\",\"symbol\":\"%s\",\"digits\":%d,\"side\":\"%s\",\"sourceSideCode\":%d,\"volumeLots\":%.2f,\"sourceVolume\":%d,\"sourceVolumeUnit\":\"MT4_CENTILOT\",\"openPrice\":%.5f,\"closePrice\":%.5f,\"sl\":%.5f,\"tp\":%.5f,\"profit\":%.2f,\"commission\":%.2f,\"swap\":%.2f,\"reason\":\"%s\",\"sourceReasonCode\":%d,\"occurredAt\":\"%s\",\"openTimeUtc\":\"%s\",\"sourceTime\":%ld,\"serverUtcOffsetSeconds\":%ld,\"comment\":\"%s\"}",
      srvId,t.login,t.order,t.order,symbol.c_str(),t.digits,SideName(t.cmd),t.cmd, t.volume/100.0,t.volume,t.open_price,t.close_price,t.sl,t.tp, t.profit,t.commission,t.storage,ReasonName((int)t.reason),(int)t.reason, occurredAt.c_str(),openUtc.c_str(),(long)t.timestamp,offset,comment.c_str());
    std::string env=std::string("{\"envelopeVersion\":1,\"platform\":\"MT4\",\"sourceServerId\":\"")+srvId+"\",\"sourceSequence\":"+std::to_string(seq++)+",\"tradingAccountId\":\""+acct+"\",\"occurredAt\":\""+occurredAt+"\",\"payload\":"+payload+"}";
    rd_kafka_resp_err_t e=rd_kafka_producev(rk,RD_KAFKA_V_TOPIC(topic),RD_KAFKA_V_KEY(acct,strlen(acct)),RD_KAFKA_V_VALUE((void*)env.data(),env.size()),RD_KAFKA_V_MSGFLAGS(RD_KAFKA_MSG_F_COPY),RD_KAFKA_V_END);
    if(e) fprintf(stderr,"produce error: %s\n",rd_kafka_err2str(e)); rd_kafka_poll(rk,0); }

  if(tr) man->MemFree(tr); man->Disconnect(); man->Release(); factory.WinsockCleanup();
  printf("Flushing Kafka...\n"); rd_kafka_flush(rk,10000); rd_kafka_destroy(rk);
  printf("DONE. produced=%d delivered=%ld failed=%ld -> %s\n",n,g_delivered,g_failed,topic); return 0; }
