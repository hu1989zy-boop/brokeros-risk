import 'dart:async';

import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:openid_client/openid_client.dart';

import '../config/app_config.dart';
import 'browser_pkce_authenticator.dart';

class AuthSession {
  const AuthSession._({required this.isAuthenticated});

  const AuthSession.authenticated() : this._(isAuthenticated: true);

  const AuthSession.unauthenticated() : this._(isAuthenticated: false);

  final bool isAuthenticated;
}

abstract interface class RefreshTokenStore {
  Future<String?> read();

  Future<void> write(String refreshToken);

  Future<void> clear();
}

class SecureRefreshTokenStore implements RefreshTokenStore {
  SecureRefreshTokenStore({FlutterSecureStorage? storage})
      : _storage = storage ?? const FlutterSecureStorage();

  static const _key = 'brokeros.risk.console.refresh-token';
  final FlutterSecureStorage _storage;

  @override
  Future<String?> read() => _storage.read(key: _key);

  @override
  Future<void> write(String refreshToken) =>
      _storage.write(key: _key, value: refreshToken);

  @override
  Future<void> clear() => _storage.delete(key: _key);
}

class KeycloakAuthGateway {
  KeycloakAuthGateway({
    required AppConfig config,
    required RefreshTokenStore tokenStore,
  })  : _config = config,
        _tokenStore = tokenStore;

  final AppConfig _config;
  final RefreshTokenStore _tokenStore;

  Client? _client;
  BrowserPkceAuthenticator? _authenticator;
  Credential? _credential;
  String? _accessToken;
  StreamSubscription<TokenResponse>? _tokenSubscription;

  String? get accessToken => _accessToken;

  Future<AuthSession> restore() async {
    await _initialize();
    final callbackCredential = await _authenticator!.credentialFromCallback();
    if (callbackCredential != null) {
      return _accept(callbackCredential, forceRefresh: false);
    }

    final storedRefreshToken = await _tokenStore.read();
    if (storedRefreshToken == null) {
      return const AuthSession.unauthenticated();
    }
    try {
      final credential = _client!.createCredential(
        refreshToken: storedRefreshToken,
      );
      return await _accept(credential, forceRefresh: true);
    } on Object {
      await _clearMemoryAndStore();
      return const AuthSession.unauthenticated();
    }
  }

  Future<void> beginLogin() async {
    await _initialize();
    _authenticator!.authorize();
  }

  Future<AuthSession> refresh() async {
    var credential = _credential;
    if (credential == null) {
      final refreshToken = await _tokenStore.read();
      if (refreshToken == null) {
        return const AuthSession.unauthenticated();
      }
      await _initialize();
      credential = _client!.createCredential(refreshToken: refreshToken);
    }
    try {
      return await _accept(credential, forceRefresh: true);
    } on Object {
      await _clearMemoryAndStore();
      return const AuthSession.unauthenticated();
    }
  }

  Future<void> logout() async {
    final authenticator = _authenticator;
    final credential = _credential;
    await _clearMemoryAndStore();
    authenticator?.logout(credential);
  }

  Future<void> _initialize() async {
    if (_client != null) {
      return;
    }
    final issuer = await Issuer.discover(Uri.parse(_config.oidcIssuer));
    _client = Client(issuer, _config.oidcClientId);
    _authenticator = BrowserPkceAuthenticator(
      _client!,
      scopes: const ['openid', 'profile'],
    );
  }

  Future<AuthSession> _accept(
    Credential credential, {
    required bool forceRefresh,
  }) async {
    final tokens = await credential.getTokenResponse(forceRefresh);
    final accessToken = tokens.accessToken;
    if (accessToken == null) {
      throw StateError('OIDC provider returned no access token');
    }
    _credential = credential;
    _accessToken = accessToken;
    await _persistRefreshToken(tokens.refreshToken ?? credential.refreshToken);
    await _tokenSubscription?.cancel();
    _tokenSubscription = credential.onTokenChanged.listen((changed) {
      _accessToken = changed.accessToken;
      unawaited(_persistRefreshToken(changed.refreshToken));
    });
    return const AuthSession.authenticated();
  }

  Future<void> _persistRefreshToken(String? refreshToken) async {
    if (refreshToken != null && refreshToken.isNotEmpty) {
      await _tokenStore.write(refreshToken);
    }
  }

  Future<void> _clearMemoryAndStore() async {
    await _tokenSubscription?.cancel();
    _tokenSubscription = null;
    _credential = null;
    _accessToken = null;
    await _tokenStore.clear();
  }
}
