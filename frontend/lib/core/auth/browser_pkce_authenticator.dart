import 'dart:convert';
import 'dart:js_interop';
import 'dart:math';

import 'package:openid_client/openid_client.dart';
import 'package:web/web.dart' as web;

class BrowserPkceAuthenticator {
  BrowserPkceAuthenticator(
    this._client, {
    this.scopes = const ['openid', 'profile'],
  });

  static const _stateKey = 'brokeros.risk.console.oidc.state';
  static const _verifierKey = 'brokeros.risk.console.oidc.verifier';

  final Client _client;
  final List<String> scopes;

  Future<Credential?> credentialFromCallback() async {
    final callbackUri = Uri.parse(web.window.location.href);
    if (callbackUri.queryParameters.containsKey('error')) {
      web.window.sessionStorage.removeItem(_stateKey);
      web.window.sessionStorage.removeItem(_verifierKey);
      web.window.history.replaceState(
        ''.toJS,
        '',
        _redirectUri(callbackUri).toString(),
      );
      throw StateError('OIDC authorization failed');
    }
    if (!callbackUri.queryParameters.containsKey('code')) {
      return null;
    }
    final state = web.window.sessionStorage.getItem(_stateKey);
    final verifier = web.window.sessionStorage.getItem(_verifierKey);
    if (state == null || verifier == null) {
      throw StateError('OIDC callback state is unavailable');
    }

    final flow = Flow.authorizationCodeWithPKCE(
      _client,
      state: state,
      codeVerifier: verifier,
      scopes: scopes,
    )..redirectUri = _redirectUri(callbackUri);
    try {
      final credential = await flow.callback(callbackUri.queryParameters);
      web.window.history.replaceState(
        ''.toJS,
        '',
        flow.redirectUri.toString(),
      );
      return credential;
    } finally {
      web.window.sessionStorage.removeItem(_stateKey);
      web.window.sessionStorage.removeItem(_verifierKey);
    }
  }

  void authorize() {
    final state = _randomUrlSafe(32);
    final verifier = _randomUrlSafe(64);
    final flow = Flow.authorizationCodeWithPKCE(
      _client,
      state: state,
      codeVerifier: verifier,
      scopes: scopes,
    )..redirectUri = _redirectUri(Uri.parse(web.window.location.href));
    web.window.sessionStorage.setItem(_stateKey, state);
    web.window.sessionStorage.setItem(_verifierKey, verifier);
    web.window.location.href = flow.authenticationUri.toString();
  }

  void logout(Credential? credential) {
    final redirectUri = _redirectUri(Uri.parse(web.window.location.href));
    final logoutUri = credential?.generateLogoutUrl(redirectUri: redirectUri);
    web.window.location.href = (logoutUri ?? redirectUri).toString();
  }

  Uri _redirectUri(Uri uri) => uri.replace(query: '', fragment: '');

  String _randomUrlSafe(int byteCount) {
    final random = Random.secure();
    final bytes = List<int>.generate(byteCount, (_) => random.nextInt(256));
    return base64UrlEncode(bytes).replaceAll('=', '');
  }
}
