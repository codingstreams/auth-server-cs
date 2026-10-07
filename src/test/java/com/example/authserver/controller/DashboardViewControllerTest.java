package com.example.authserver.controller;

import com.example.authserver.controller.dto.UpdateUserProfileRequest;
import com.example.authserver.controller.dto.UserProfileUpdateResponse;
import com.example.authserver.service.appuser.AppUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardViewControllerTest {

  private AppUserService stubAppUserService;
  private DashboardViewController dashboardViewController;
  private final AtomicReference<UpdateUserProfileRequest> capturedRequest = new AtomicReference<>();

  @BeforeEach
  void setUp() {
    capturedRequest.set(null);

    stubAppUserService = (AppUserService) Proxy.newProxyInstance(
        AppUserService.class.getClassLoader(),
        new Class<?>[]{AppUserService.class},
        (proxy, method, args) -> {
          if ("updateUserProfile".equals(method.getName())) {
            final UpdateUserProfileRequest req = (UpdateUserProfileRequest) args[0];
            capturedRequest.set(req);
            return new UserProfileUpdateResponse(req.fullName());
          }
          return null;
        }
    );

    dashboardViewController = new DashboardViewController(stubAppUserService);
  }

  @Test
  @DisplayName("updateName delegates to appUserService and redirects to dashboard tab-general")
  void testUpdateName() {
    final String newDisplayName = "Bob Smith";
    final String viewName = dashboardViewController.updateName(newDisplayName);

    assertThat(viewName).isEqualTo("redirect:/dashboard?updated=name#tab-general");
    assertThat(capturedRequest.get()).isNotNull();
    assertThat(capturedRequest.get().fullName()).isEqualTo(newDisplayName);
  }
}
