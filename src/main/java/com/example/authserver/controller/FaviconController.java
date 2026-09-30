package com.example.authserver.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Slf4j
//@Controller
@RequiredArgsConstructor
public class FaviconController {

//  @GetMapping("/favicon.ico")
//  @ResponseBody
//  public ResponseEntity<Void> getFavicon() {
//    log.trace("Favicon requested, returning 204 No Content");
//    return ResponseEntity.noContent().build();
//  }
}
