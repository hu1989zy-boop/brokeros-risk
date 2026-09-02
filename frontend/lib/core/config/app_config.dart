class AppConfig {
  const AppConfig({
    required this.apiBaseUrl,
    required this.oidcIssuer,
    required this.oidcClientId,
  });

  factory AppConfig.fromEnvironment() {
    const apiBaseUrl = String.fromEnvironment(
      'BROKEROS_API_BASE_URL',
      defaultValue: 'http://localhost:8080',
    );
    const oidcIssuer = String.fromEnvironment(
      'BROKEROS_OIDC_ISSUER',
      defaultValue: 'http://localhost:8180/realms/brokeros',
    );
    const oidcClientId = String.fromEnvironment(
      'BROKEROS_OIDC_CLIENT_ID',
      defaultValue: 'brokeros-risk-console',
    );
    return const AppConfig(
      apiBaseUrl: apiBaseUrl,
      oidcIssuer: oidcIssuer,
      oidcClientId: oidcClientId,
    );
  }

  final String apiBaseUrl;
  final String oidcIssuer;
  final String oidcClientId;
}
