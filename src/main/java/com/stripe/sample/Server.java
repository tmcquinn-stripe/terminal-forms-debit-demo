package com.stripe.sample;

import java.nio.file.Paths;

import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.staticFiles;
import static spark.Spark.port;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.param.PaymentIntentCreateParams;
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
import java.util.HashMap;

public class Server {
  private static Gson gson = new Gson();

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
    port(4242);
    staticFiles.externalLocation(Paths.get("public").toAbsolutePath().toString());

    // This is a public sample test API key.
    // Don’t submit any personally identifiable information in requests made with this key.
    // Sign in to see your own test API key embedded in code samples.
    Stripe.apiKey = "***";
    Stripe.stripeVersion += ";terminal_collect_confirm_beta=v1";

    post("/create_location", (request, response) -> {
      LocationCreateParams.Address address =
      LocationCreateParams.Address.builder()
        .setLine1("3040 Bur Oak Ave")
        .setCity("Markham")
        .setState("ON")
        .setCountry("CA")
        .setPostalCode("L6B 0R1")
        .build();

      LocationCreateParams params =
        LocationCreateParams.builder()
          .setDisplayName("HQ")
          .setAddress(address)
          .build();

      Location location = Location.create(params);
      return location.toJson();
    });


    post("/register_reader", (request, response) -> {
      ReaderParams postBody = gson.fromJson(request.body(), ReaderParams.class);

      //naughty hardcode

      Reader reader = Reader.retrieve("tmr_EddsAANa8qQXwu");

      return reader.toJson();
    });

    post("/create_payment_intent", (request, response) -> {
      response.type("application/json");

      PaymentIntentParams postBody = gson.fromJson(request.body(), PaymentIntentParams.class);

      // For Terminal payments, the 'payment_method_types' parameter must include
      // 'card_present'.
      // To automatically capture funds when a charge is authorized,
      // set `capture_method` to `automatic`.
      PaymentIntentCreateParams createParams = PaymentIntentCreateParams.builder()
        .setCurrency("usd")
        .setAmount(postBody.getAmount())
        .setCaptureMethod(PaymentIntentCreateParams.CaptureMethod.MANUAL)
        .addPaymentMethodType("card_present")
        .build();
      // Create a PaymentIntent with the order amount and currency
      PaymentIntent intent = PaymentIntent.create(createParams);

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
        System.out.println("After");
        System.out.println(reader.toJson());
        return reader.toJson();
    });

    post("/confirm_payment", (request, response) -> {
      ProcessPaymentParams postBody = gson.fromJson(request.body(), ProcessPaymentParams.class);

      ReaderConfirmPaymentIntentParams params =  ReaderConfirmPaymentIntentParams.builder()
        .setPaymentIntent(postBody.getPaymentIntentId())
        .build();

      Reader reader = Reader.retrieve(postBody.getReaderId());
      reader = reader.confirmPaymentIntent(params);

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
          .setNumber("4242424242424242")
          .build())
        .build(); 

        Reader reader = Reader.retrieve(postBody.getReaderId());
        reader = reader.getTestHelpers().presentPaymentMethod(params);

        return reader.toJson();
    });

    post("/retrieve_reader", (request, response) -> {
      ProcessPaymentParams postBody = gson.fromJson(request.body(), ProcessPaymentParams.class);

     // Stripe.overrideApiBase("http://localhost:4444"); 
      Reader reader = Reader.retrieve(postBody.getReaderId());


      String pm_id = reader.getAction().getCollectPaymentMethod().getPaymentMethod().getId();

      PaymentMethod pm = PaymentMethod.retrieve(pm_id);
      System.out.println(pm.getCardPresent().toString());

      System.out.println(reader.getAction().getCollectPaymentMethod().getPaymentMethod().getCardPresent().toString());

      return reader.toJson();
    });

    post("/process_payment", (request, response) -> {
      ProcessPaymentParams postBody = gson.fromJson(request.body(), ProcessPaymentParams.class);

      ReaderProcessPaymentIntentParams params =
        ReaderProcessPaymentIntentParams.builder()
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
      intent = intent.capture();

      return intent.toJson();
    });

    post("/signature_form", (request, response) -> {
      response.type("application/json");
      ReaderParams postBody = gson.fromJson(request.body(), ReaderParams.class);

      CustomText customText = CustomText.builder()
        .setTitle("Signature")
        .setDescription("Something")
        .setSubmitButton("submit")
        .setSkipButton("skip")
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
        .setTitle("Something Something")
        .setDescription("Big Cool Something")
        .build();

      Selection selections = Selection.builder()
        .addChoice(Choice.builder()
            .setValue("Choice 1")
            .setStyle(Style.PRIMARY)
            .build())
        .addChoice(Choice.builder()
            .setValue("Choice 2")
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