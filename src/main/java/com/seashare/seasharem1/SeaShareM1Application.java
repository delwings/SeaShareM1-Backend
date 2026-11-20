package com.seashare.seasharem1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Punto de entrada del Módulo 1 (gestión de flota y embarcaciones). */
@SpringBootApplication
@ConfigurationPropertiesScan
public class SeaShareM1Application {

  public static void main(String[] args) {
    SpringApplication.run(SeaShareM1Application.class, args);
  }
}
