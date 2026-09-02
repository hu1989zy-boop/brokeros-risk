import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/api/api_client.dart';
import '../../../core/auth/auth_controller.dart';
import '../data/risk_case_repository.dart';

final apiClientProvider = Provider<ApiClient>((ref) {
  return ApiClient(
    baseUrl: ref.watch(appConfigProvider).apiBaseUrl,
    auth: ref.read(authControllerProvider.notifier),
  );
});

final riskCaseRepositoryProvider = Provider<RiskCaseRepository>((ref) {
  return DioRiskCaseRepository(ref.watch(apiClientProvider));
});
