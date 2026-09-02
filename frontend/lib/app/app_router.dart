import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../core/auth/auth_controller.dart';
import '../core/auth/login_page.dart';
import '../features/riskcase/presentation/risk_case_detail_page.dart';
import '../features/riskcase/presentation/risk_case_list_page.dart';

final appRouterProvider = Provider<GoRouter>((ref) {
  final auth = ref.watch(authControllerProvider);
  final authenticated = auth.value?.isAuthenticated ?? false;

  return GoRouter(
    initialLocation: '/cases',
    redirect: (context, state) {
      final onLogin = state.matchedLocation == '/login';
      if (!authenticated && !onLogin) {
        return '/login';
      }
      if (authenticated && onLogin) {
        return '/cases';
      }
      return null;
    },
    routes: [
      GoRoute(
        path: '/login',
        builder: (context, state) => const LoginPage(),
      ),
      GoRoute(
        path: '/cases',
        builder: (context, state) => const RiskCaseListPage(),
        routes: [
          GoRoute(
            path: ':caseNumber',
            builder: (context, state) => RiskCaseDetailPage(
              caseNumber: state.pathParameters['caseNumber']!,
            ),
          ),
        ],
      ),
    ],
  );
});
