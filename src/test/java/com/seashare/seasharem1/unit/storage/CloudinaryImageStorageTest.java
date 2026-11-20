package com.seashare.seasharem1.unit.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.seashare.seasharem1.application.port.out.StoredImage;
import com.seashare.seasharem1.domain.exception.ExternalServiceUnavailableException;
import com.seashare.seasharem1.infrastructure.adapter.out.storage.CloudinaryImageStorage;
import com.seashare.seasharem1.infrastructure.adapter.out.storage.CloudinaryProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class CloudinaryImageStorageTest {

  private static final Clock CLOCK =
      Clock.fixed(Instant.ofEpochSecond(1_700_000_000L), ZoneOffset.UTC);
  private static final CloudinaryProperties CONFIGURED =
      new CloudinaryProperties("mi-nube", "123456", "mi-secreto");

  @Test
  void signatureIsSha1OfSortedParametersPlusSecret() {
    assertEquals(
        "d325e0d12ab9ed86b24ab942dc26cef72b2fd32e",
        CloudinaryImageStorage.sign("folder=seashare/vessels&timestamp=1700000000", "mi-secreto"));
  }

  @Test
  void returnsThePublicUrlWhenCloudinaryAccepts() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server
        .expect(requestTo("https://api.cloudinary.com/v1_1/mi-nube/image/upload"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(
            withSuccess(
                "{\"secure_url\":\"https://res.cloudinary.com/mi-nube/a.jpg\",\"public_id\":\"seashare/vessels/a\"}",
                MediaType.APPLICATION_JSON));
    CloudinaryImageStorage storage = new CloudinaryImageStorage(CONFIGURED, builder.build(), CLOCK);

    StoredImage image = storage.store(new byte[] {1, 2, 3}, "image/jpeg", "seashare/vessels");

    assertEquals("https://res.cloudinary.com/mi-nube/a.jpg", image.url());
    assertEquals("seashare/vessels/a", image.publicId());
    server.verify();
  }

  @Test
  void cloudinaryFailureBecomesAnExternalServiceError() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer.bindTo(builder)
        .build()
        .expect(requestTo("https://api.cloudinary.com/v1_1/mi-nube/image/upload"))
        .andRespond(withServerError());
    CloudinaryImageStorage storage = new CloudinaryImageStorage(CONFIGURED, builder.build(), CLOCK);

    assertThrows(
        ExternalServiceUnavailableException.class,
        () -> storage.store(new byte[] {1}, "image/png", "seashare/vessels"));
  }

  @Test
  void withoutCredentialsItFailsClearlyInsteadOfCallingOut() {
    CloudinaryImageStorage storage =
        new CloudinaryImageStorage(
            new CloudinaryProperties("", "", ""), RestClient.create(), CLOCK);

    assertThrows(
        ExternalServiceUnavailableException.class,
        () -> storage.store(new byte[] {1}, "image/png", "seashare/vessels"));
  }
}
