# Q-015 Phase B — AWS x64 Windows Gateway Host (ops note)

Date: 2026-09-07
Purpose: a reusable runbook for provisioning the **x64 Windows** host that Q-015
Phase B's MT4 (and later MT5) gateway will run on. Companion to
`docs/2026-09-07-q015-phase-b-mt4-sdk-intake-checklist.md` (step 2). Ops guidance only
— it contains **no** MT4/MT5 SDK detail and invents no Manager API interface (AGENTS.md).

> You create the AWS account, key pair, and enter all credentials yourself (assistant
> safety rule). The values in `<...>` are yours to fill. Never place MT4/MT5 Manager
> credentials or any account-holder PII in this repo, logs, or fixtures.

## 1. Launch parameters

| Item | Value | Notes |
| --- | --- | --- |
| **AMI** | Windows Server 2022 Base (English, Full) | Resolve the current image via the AWS-published SSM parameter, do **not** hard-code an AMI ID: `/aws/service/ami-windows-latest/Windows_Server-2022-English-Full-Base` |
| **Architecture** | **x86_64 (amd64)** | Not ARM/Graviton. A 32-bit MT4 DLL still runs on x64 Windows via WOW64. |
| **Instance type** | `t3.large` (2 vCPU / 8 GB) | Start here; `t3.xlarge` if needed. Avoid `t4g.*` (ARM). |
| **Disk** | 80 GB `gp3` EBS | Windows base ~30 GB + JDK + SDK + samples. |
| **Key pair** | a new `.pem` | Used to decrypt the Administrator password for RDP. |
| **SG inbound** | **RDP 3389 ← `<YOUR_IP>/32` only** | Do not open `0.0.0.0/0`. |
| **SG outbound** | to the **MT4 demo server host:port** | Confirm the MT4 manager connection port with your broker (often 443 or a broker-specified port). Allow-all outbound is acceptable for a dev box. |
| **Region** | closest to the MT4 demo server | Lower pumping latency. |
| **IAM (optional)** | role with `AmazonSSMManagedInstanceCore` | Enables SSM Fleet Manager login without opening 3389. |

## 2. Launch via CLI (run from your own machine with AWS CLI configured)

Resolve the current AMI ID:

```bash
aws ssm get-parameter \
  --name /aws/service/ami-windows-latest/Windows_Server-2022-English-Full-Base \
  --query 'Parameter.Value' --output text --region <REGION>
```

Launch the instance:

```bash
aws ec2 run-instances \
  --image-id <AMI_ID_FROM_ABOVE> \
  --instance-type t3.large \
  --key-name <YOUR_KEYPAIR> \
  --security-group-ids <YOUR_SG_ID> \
  --subnet-id <YOUR_SUBNET_ID> \
  --block-device-mappings '[{"DeviceName":"/dev/sda1","Ebs":{"VolumeSize":80,"VolumeType":"gp3"}}]' \
  --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=mt4-gateway-dev},{Key=project,Value=brokeros-risk-q015}]' \
  --region <REGION>
```

Restrict RDP to your IP:

```bash
aws ec2 authorize-security-group-ingress \
  --group-id <YOUR_SG_ID> --protocol tcp --port 3389 \
  --cidr <YOUR_PUBLIC_IP>/32 --region <REGION>
```

## 3. First-boot Windows setup

1. Get the Administrator password: EC2 console → the instance → **Connect → RDP
   client → Get password** (use your `.pem`), then RDP in.
2. Install **JDK 21** (x64). **If the MT4 Manager API is 32-bit** (checklist step 0),
   also install a **32-bit JDK 21** + the matching **32-bit MSVC redistributable** per
   the package's documented requirement — the MT4 gateway process must then be 32-bit.
3. Install **Git**; `git clone` this repo (Phase A platform-side code is here).
4. Take an **AMI snapshot** of this baseline so the host can be rebuilt on demand.

## 4. Cost & security hygiene

- **Stop** the instance when idle; **Terminate** when done (EBS bills while stopped).
- Demo environments are disposable — tear down after use.
- Keep RDP to your IP only; prefer **SSM Fleet Manager** to avoid opening 3389 at all.
- No Manager credentials / PII in the repo, logs, or fixtures — ever.

## 5. Next

Once the host is up and the official MT4 sample connects to the demo server (checklist
step 3) and you have captured the real structures + de-identified samples (step 4),
bring those back to start **Phase B (MT4)** under governance. The same host serves the
MT5 gateway later; MT5's bitness/runtime requirements are confirmed separately when you
do the MT5 intake.
