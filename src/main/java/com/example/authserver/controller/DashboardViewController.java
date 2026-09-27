package com.example.authserver.controller;

import com.example.authserver.controller.dto.UpdateUserProfileRequest;
import com.example.authserver.service.appuser.AppUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
@RequiredArgsConstructor
public class DashboardViewController {

  private final AppUserService appUserService;

  @PostMapping("/profile/update-name")
  public String updateName(@RequestParam("displayName") String displayName) {
    log.info("Received request to update display name: {}", displayName);
    final var response = appUserService.updateUserProfile(UpdateUserProfileRequest.updateName(displayName));
    log.info("Updated display name successfully to: {}", response.fullName());

    return "redirect:/success?updated=name#tab-general";
  }
}

