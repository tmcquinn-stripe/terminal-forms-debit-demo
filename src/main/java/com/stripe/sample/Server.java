package com.stripe.sample;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.file.Paths;

import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.staticFiles;
import static spark.Spark.port;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import com.stripe.Stripe;
import com.stripe.model.File;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentIntentCreateParams.PaymentDetails;
import com.stripe.param.PaymentIntentCreateParams.PaymentDetails.CarRental;

import com.stripe.param.PaymentIntentCreateParams.PaymentMethodOptions;
import com.stripe.param.PaymentIntentCreateParams.PaymentMethodOptions.Card;
import com.stripe.param.PaymentIntentCreateParams.PaymentMethodOptions.Card.StatementDetails;
import com.stripe.param.PaymentIntentCreateParams.PaymentMethodOptions.Card.StatementDetails.Address;


import com.stripe.param.EphemeralKeyCreateParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentIntentUpdateParams;
import com.stripe.model.terminal.Reader;
import com.stripe.model.terminal.ReaderCollection;
import com.stripe.model.terminal.Reader.TestHelpers;
import com.stripe.net.RequestOptions;
import com.stripe.param.terminal.ReaderProcessPaymentIntentParams;
import com.stripe.param.terminal.ReaderCollectInputsParams.Input;
import com.stripe.param.terminal.ReaderCollectInputsParams.Input.CustomText;
import com.stripe.param.terminal.ReaderCollectInputsParams.Input.Selection;
import com.stripe.param.terminal.ReaderCollectInputsParams.Input.Selection.Choice;
import com.stripe.param.terminal.ReaderCollectInputsParams.Input.Selection.Choice.Style;
import com.stripe.param.terminal.ReaderCollectPaymentMethodParams.CollectConfig;
import com.stripe.param.terminal.ReaderPresentPaymentMethodParams.CardPresent;
import com.stripe.param.terminal.ReaderPresentPaymentMethodParams.Type;
import com.stripe.param.terminal.ReaderCreateParams;
import com.stripe.param.terminal.ReaderPresentPaymentMethodParams;
import com.stripe.param.terminal.ConnectionTokenCreateParams;
import com.stripe.model.terminal.Location;
import com.stripe.param.terminal.LocationCreateParams;
import com.stripe.param.terminal.ReaderCancelActionParams;
import com.stripe.param.terminal.ReaderCollectInputsParams;
import com.stripe.param.terminal.ReaderCollectPaymentMethodParams;
import com.stripe.param.terminal.ReaderConfirmPaymentIntentParams;
import com.stripe.exception.StripeException;
import java.util.Map;
import javax.net.ssl.HttpsURLConnection;
import java.util.HashMap;


public class Server {
  private static Gson gson = new Gson();

  public static void getFileContents(String strUrl) throws Exception {
    // Create URL object
    URL url = new URL(strUrl);

    // Create HttpsURLConnection object
    HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

    // Set request method
    connection.setRequestMethod("GET");

    // Set timeouts
    connection.setConnectTimeout(5000);
    connection.setReadTimeout(5000);

    // Send GET request
    int responseCode = connection.getResponseCode();
    System.out.println("Response Code: " + responseCode);

    // Read response
    InputStream inputStream = connection.getInputStream();
    String fileName = "response.txt";
    writeResponseToFile(inputStream, fileName);

    // Disconnect the connection
    connection.disconnect();
  }

  private static void writeResponseToFile(InputStream inputStream, String fileName) throws IOException {
    try (OutputStream outputStream = new FileOutputStream(fileName)) {
      byte[] buffer = new byte[4096];
      int bytesRead;
      while ((bytesRead = inputStream.read(buffer)) != -1) {
        outputStream.write(buffer, 0, bytesRead);
      }
    }
    System.out.println("Response has been written to the file: " + fileName);
  }

  static class PaymentIntentParams {
    private String payment_intent_id;
    private long amount;

    public String getPaymentIntentId() {
      return payment_intent_id;
    }

    public long getAmount() {
      return amount;
    }
  }

  static class ReaderParams {
    private String reader_id;
    private String location_id;

    public String getReaderId() {
      return reader_id;
    }

    public String getLocationId() {
      return location_id;
    }
  }

  static class ProcessPaymentParams {
    private String reader_id;
    private String payment_intent_id;

    public String getReaderId() {
      return reader_id;
    }

    public String getPaymentIntentId() {
      return payment_intent_id;
    }
  }

  public static void main(String[] args) {
    port(4848);
    staticFiles.externalLocation(Paths.get("public").toAbsolutePath().toString());

    // This is a public sample test API key.
    // Don’t submit any personally identifiable information in requests made with
    // this key.
    // Sign in to see your own test API key embedded in code samples.
    Stripe.apiKey = "{{REPLACE WITH KEY}}";
    Stripe.stripeVersion += ";terminal_collect_confirm_beta=v1;terminal_collect_inputs_beta=v1";

    post("/create_location", (request, response) -> {
      LocationCreateParams.Address address = LocationCreateParams.Address.builder()
          .setLine1("3040 Bur Oak Ave")
          .setCity("Markham")
          .setState("ON")
          .setCountry("CA")
          .setPostalCode("L6B 0R1")
          .build();

      LocationCreateParams params = LocationCreateParams.builder()
          .setDisplayName("HQ")
          .setAddress(address)
          .build();

      Location location = Location.create(params);
      return location.toJson();
    });

    post("/register_reader", (request, response) -> {
   //   ReaderParams postBody = gson.fromJson(request.body(), ReaderParams.class);

      // naughty hardcode

      Reader reader = Reader.retrieve("tmr_FNnmNgaVKRIGfw");

      // reader.getAction().getCollectInputs().getInputs().get(0).getSignature().getValue();

      return reader.toJson();
    });

    post("/create_payment_intent", (request, response) -> {
      response.type("application/json");

      PaymentIntentParams postBody = gson.fromJson(request.body(), PaymentIntentParams.class);

      Stripe.stripeVersion = "2022-11-15";

      // PaymentMethodOptions pmo = PaymentMethodOptions.builder(

   //   RequestOptions requestOptions = RequestOptions.builder().setStripeAccount("acct_1Lv6C3FVs0uW6boV")
      //    .build();
      // For Terminal payments, the 'payment_method_types' parameter must include
      // 'card_present'.
      // To automatically capture funds when a charge is authorized,
      // set `capture_method` to `automatic`.

      PaymentIntentCreateParams createParams = PaymentIntentCreateParams.builder()
          .setCurrency("usd")
          .setAmount(postBody.getAmount())
          .addPaymentMethodType("card_present")
          .build();
      // Create a PaymentIntent with the order amount and currency
      PaymentIntent intent = PaymentIntent.create(createParams);

      Stripe.stripeVersion += ";terminal_collect_confirm_beta=v1;terminal_collect_inputs_beta=v1";

      return intent.toJson();
    });

    post("/collect_payment_method", (request, response) -> {
      ProcessPaymentParams postBody = gson.fromJson(request.body(), ProcessPaymentParams.class);

      ReaderCollectPaymentMethodParams params = ReaderCollectPaymentMethodParams.builder()
          .setPaymentIntent(postBody.getPaymentIntentId())
          .build();

      Reader reader = Reader.retrieve(postBody.getReaderId());
      System.out.println(reader.toJson());

      reader = reader.collectPaymentMethod(params);

      return reader.toJson();
    });

    post("/confirm_payment", (request, response) -> {
      ProcessPaymentParams postBody = gson.fromJson(request.body(), ProcessPaymentParams.class);
      ReaderConfirmPaymentIntentParams params = ReaderConfirmPaymentIntentParams.builder()
          .setPaymentIntent(postBody.getPaymentIntentId())
          .build();

      PaymentIntent pi = PaymentIntent.retrieve(postBody.getPaymentIntentId());

      pi.update(PaymentIntentUpdateParams.builder().setAmount((long) 1000).build());

      Reader reader = Reader.retrieve(postBody.getReaderId());
      reader = reader.confirmPaymentIntent(params);

      System.out.println("HELLO");
      System.out.println(pi.getLatestCharge());

      return reader.toJson();
    });

    post("/cancel", (request, response) -> {
      ProcessPaymentParams postBody = gson.fromJson(request.body(), ProcessPaymentParams.class);

      Reader reader = Reader.retrieve(postBody.getReaderId());

      reader = reader.cancelAction();

      PaymentIntent intent = PaymentIntent.retrieve(postBody.getPaymentIntentId());
      intent = intent.cancel();

      return reader.toJson();

    });

    post("/present_payment_method", (request, response) -> {

      ProcessPaymentParams postBody = gson.fromJson(request.body(), ProcessPaymentParams.class);

      ReaderPresentPaymentMethodParams params = ReaderPresentPaymentMethodParams.builder()
          .setType(Type.CARD_PRESENT)
          .setCardPresent(CardPresent.builder()
              .setNumber("4761739001010010")
              .build())
          .build();

      Reader reader = Reader.retrieve(postBody.getReaderId());
      reader = reader.getTestHelpers().presentPaymentMethod(params);

      return reader.toJson();
    });

    post("/retrieve_reader", (request, response) -> {
      ProcessPaymentParams postBody = gson.fromJson(request.body(), ProcessPaymentParams.class);
      Reader reader = Reader.retrieve(postBody.getReaderId());
      return reader.toJson();
    });

    post("/process_payment", (request, response) -> {
      ProcessPaymentParams postBody = gson.fromJson(request.body(), ProcessPaymentParams.class);

      ReaderProcessPaymentIntentParams params = ReaderProcessPaymentIntentParams.builder()
          .setPaymentIntent(postBody.getPaymentIntentId())
          .build();

      Reader reader = Reader.retrieve(postBody.getReaderId());
      reader = reader.processPaymentIntent(params);
      return reader.toJson();
    });

    post("/simulate_payment", (request, response) -> {
      ReaderParams postBody = gson.fromJson(request.body(), ReaderParams.class);

      Reader reader = Reader.retrieve(postBody.getReaderId());
      reader = reader.getTestHelpers().presentPaymentMethod();
      return reader.toJson();
    });

    post("/capture_payment_intent", (request, response) -> {
      response.type("application/json");

      PaymentIntentParams postBody = gson.fromJson(request.body(), PaymentIntentParams.class);

      PaymentIntent intent = PaymentIntent.retrieve(postBody.getPaymentIntentId());

      System.out.println(intent.getLatestCharge());

      intent = intent.capture();

      return intent.toJson();
    });

    post("/signature_form", (request, response) -> {
      response.type("application/json");
      ReaderParams postBody = gson.fromJson(request.body(), ReaderParams.class);

      CustomText customText = CustomText.builder()
          .setTitle("Please sign")
          .setDescription("Sign to confirm you have read and agreed to the terms and conditions")
          .setSubmitButton("Submit")
          .setSkipButton("Cancel")
          .build();

      Input input = Input.builder()
          .setType(ReaderCollectInputsParams.Input.Type.SIGNATURE)
          .setCustomText(customText)
          .setRequired(false)
          .build();

      ReaderCollectInputsParams readerCollectInputParams = ReaderCollectInputsParams.builder()
          .addInput(input)
          .putMetadata("ra_number", "ra_123")
          .build();

      Reader reader = Reader.retrieve(postBody.getReaderId());

      reader = reader.collectInputs(readerCollectInputParams);

      return reader.toJson();
    });

    post("/selection_form", (request, response) -> {
      response.type("application/json");
      ReaderParams postBody = gson.fromJson(request.body(), ReaderParams.class);

      CustomText customText = CustomText.builder()
          .setTitle("Review Contract")
          .setDescription(
              "SESHU PANDY, please acknowledge that you consent to receiving the digital terms and conditions, rental related documents, and Rental Agreement for RA00001 and furthermore that you agree to the terms and conditions as described in these documents.")
          .build();

      Selection selections = Selection.builder()
          .addChoice(Choice.builder()
              .setValue("Accept")
              .setStyle(Style.PRIMARY)
              .build())
          .addChoice(Choice.builder()
              .setValue("Decline")
              .setStyle(Style.SECONDARY)
              .build())
          .build();

      Input input = Input.builder()
          .setType(ReaderCollectInputsParams.Input.Type.SELECTION)
          .setCustomText(customText)
          .setSelection(selections)
          .setRequired(true)
          .build();

      ReaderCollectInputsParams rp = ReaderCollectInputsParams.builder()
          .addInput(input)
          .putMetadata("ra_number", "ra_123")
          .build();

      Reader reader = Reader.retrieve(postBody.getReaderId());

      reader = reader.collectInputs(rp);

      return reader.toJson();
    });
  }

}