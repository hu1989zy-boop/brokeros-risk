import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../api/api_client.dart';
import '../config/app_config.dart';
import 'auth_gateway.dart';

final appConfigProvider = Provider<AppConfig>(
  (ref) => AppConfig.fromEnvironment(),
);

final refreshTokenStoreProvider = Provider<RefreshTokenStore>(
  (ref) => SecureRefreshTokenStore(),
);

final keycloakAuthGatewayProvider = Provider<KeycloakAuthGateway>((ref) {
  return KeycloakAuthGateway(
    config: ref.watch(appConfigProvider),
    tokenStore: ref.watch(refreshTokenStoreProvider),
  );
});

final authControllerProvider =
    AsyncNotifierProvider<AuthController, AuthSession>(AuthController.new);

class AuthController extends AsyncNotifier<AuthSession>
    implements AuthTokenDelegate {
  KeycloakAuthGateway get _gateway => ref.read(keycloakAuthGatewayProvider);

  @override
  Future<AuthSession> build() => _gateway.restore();

  @override
  String? get accessToken => _gateway.accessToken;

  Future<void> login() async {
    state = const AsyncLoading<AuthSession>();
    try {
      await _gateway.beginLogin();
    } on Object catch (error, stackTrace) {
      state = AsyncError<AuthSession>(error, stackTrace);
    }
  }

  @override
  Future<String?> refresh() async {
    final session = await _gateway.refresh();
    state = AsyncData<AuthSession>(session);
    return session.isAuthenticated ? _gateway.accessToken : null;
  }

  Future<void> logout() async {
    await _gateway.logout();
    state = const AsyncData<AuthSession>(AuthSession.unauthenticated());
  }
}
