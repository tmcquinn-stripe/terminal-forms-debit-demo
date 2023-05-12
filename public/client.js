function createLocation() {
  return fetch("/create_location", { method: "POST" }).then((response) => {
    return response.json();
  });
}

function createReader() {
  console.log("epeaser")
  return fetch("/register_reader", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ location_id: locationId }),
  }).then((response) => {
    console.log("hellooo")
    return response.json();
  });
}

function createPaymentIntent(amount) {
  return fetch("/create_payment_intent", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ amount: amount }),
  }).then(function(response) {
    return response.json();
  });
}

function processPayment() {
  return fetch("/process_payment", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      reader_id: readerId,
      payment_intent_id: paymentIntentId,
    }),
  }).then((response) => {
    return response.json();
  });
}

function collectPaymentMethod() {
  return fetch("/collect_payment_method", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      reader_id: readerId,
      payment_intent_id: paymentIntentId,
    }),
  }).then((response) => {
    return response.json();
  });
}

function presentPaymentMethod() {
  return fetch("/present_payment_method", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      reader_id: readerId,
      payment_intent_id: paymentIntentId,
    }),
  }).then((response) => {
    return response.json();
  });
}

function retrieveReader() {
  return fetch("/retrieve_reader", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      reader_id: readerId,
      payment_intent_id: paymentIntentId,
    }),
  }).then((response) => {
    return response.json();
  });
}


function confirmPaymentIntent() {
  return fetch("/confirm_payment", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      reader_id: readerId,
      payment_intent_id: paymentIntentId,
    }),
  }).then((response) => {
    return response.json();
  });
}

function cancel() {
  return fetch("/cancel", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      reader_id: readerId,
      payment_intent_id: paymentIntentId,
    }),
  }).then((response) => {
    return response.json();
  });
}

function signatureFormReq() {
  return fetch("/signature_form", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      reader_id: readerId,
    }),
  }).then((response) => {
    return response.json();
  });
}

function selectionFormReq() {
  return fetch("/selection_form", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      reader_id: readerId,
    }),
  }).then((response) => {
    return response.json();
  });
}

function capture(paymentIntentId) {
  return fetch("/capture_payment_intent", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ payment_intent_id: paymentIntentId }),
  }).then((response) => {
    return response.json();
  });
}

var locationId;
var readerId;
var paymentIntentId;

const createLocationButton = document.getElementById("create-location-button");
createLocationButton.addEventListener("click", async (event) => {
  createLocation().then((location) => {
    log("POST /v1/terminal/locations", location);
    locationId = location["id"];
  });
});

const createReaderButton = document.getElementById("create-reader-button");
createReaderButton.addEventListener("click", async (event) => {
  createReader().then((reader) => {
    log("POST /v1/terminal/readers", reader);
    readerId = reader["id"];
  });
});

const signatureForm = document.getElementById("signature-form-button");
signatureForm.addEventListener("click", async (event) => {
  signatureFormReq().then((reader) => {
    log("POST /v1/terminal/readers", reader);
    readerId = reader["id"];
  });
});

const selectionForm = document.getElementById("selection-form-button");
selectionForm.addEventListener("click", async (event) => {
  selectionFormReq().then((reader) => {
    log("POST /v1/terminal/readers", reader);
    readerId = reader["id"];
  });
});

const collectPaymentMethodButton = document.getElementById("collect-payment-method-button");
collectPaymentMethodButton.addEventListener("click", async (event) => {
  amount = document.getElementById("amount-input").value;
  createPaymentIntent(amount).then(function(paymentIntent) {
    log("POST /v1/payment_intents", paymentIntent);
    paymentIntentId = paymentIntent["id"];

    collectPaymentMethod().then(function(reader) {
      log("POST /v1/terminal/readers/" + readerId + "/collect_payment_method", reader);


      presentPaymentMethod().then(function(reader) {
        log("POST /v1/test_helpers/terminal/readers/" + readerId + "/present_payment_method", reader);
    })
  })
})
})

const retreiveReaderButton = document.getElementById("retrieve-reader-button");
retreiveReaderButton.addEventListener("click", async(event) => {
  retrieveReader().then(function(reader) {
    log("POST /v1/terminal/readers/" + readerId, reader);

  })
})

const confirmPaymentIntentButton = document.getElementById("confirm-payment-button");
confirmPaymentIntentButton.addEventListener("click", async (event) => {
  confirmPaymentIntent().then(function(reader) {
    log("POST /v1/terminal/readers/" + readerId + "/confirm_payment_intent", reader);
  })
})

const cancelButton = document.getElementById("cancel-button");
cancelButton.addEventListener("click", async (event) => {
  cancel().then(function(reader) {
    log("POST /v1/terminal/readers/" + readerId + "/cancel_action", reader);
  })
})

const processPaymentButton = document.getElementById("process-payment-button");
processPaymentButton.addEventListener("click", async (event) => {
  amount = document.getElementById("amount-input").value;
  createPaymentIntent(amount).then(function(paymentIntent) {
    log("POST /v1/payment_intents", paymentIntent);
    paymentIntentId = paymentIntent["id"];

    processPayment().then(function(reader) {
      log("POST /v1/terminal/readers/" + readerId + "/process_payment_intent", reader);

      simulatePayment().then(function(reader) {
        log("POST /v1/test_helpers/terminal/readers/" + readerId + "/present_payment_method", reader);
      });
    });
  });
});

const captureButton = document.getElementById("capture-button");
captureButton.addEventListener("click", async (event) => {
  capture(paymentIntentId).then((paymentIntent) => {
    log("POST /v1/payment_intents/" + paymentIntentId + "/capture", paymentIntent);
  });
});

function log(method, message) {
  var logs = document.getElementById("logs");
  var title = document.createElement("div");
  var log = document.createElement("div");
  var lineCol = document.createElement("div");
  var logCol = document.createElement("div");
  title.classList.add("row");
  title.classList.add("log-title");
  title.textContent = method;
  log.classList.add("row");
  log.classList.add("log");
  var hr = document.createElement("hr");
  var pre = document.createElement("pre");
  var code = document.createElement("code");
  code.textContent = formatJson(JSON.stringify(message, undefined, 2));
  pre.append(code);
  log.append(pre);
  logs.prepend(hr);
  logs.prepend(log);
  logs.prepend(title);
}

function stringLengthOfInt(number) {
  return number.toString().length;
}

function padSpaces(lineNumber, fixedWidth) {
  // Always indent by 2 and then maybe more, based on the width of the line
  // number.
  return " ".repeat(2 + fixedWidth - stringLengthOfInt(lineNumber));
}

function formatJson(message) {
  var lines = message.split("\n");
  var json = "";
  var lineNumberFixedWidth = stringLengthOfInt(lines.length);
  for (var i = 1; i <= lines.length; i += 1) {
    line = i + padSpaces(i, lineNumberFixedWidth) + lines[i - 1];
    json = json + line + "\n";
  }
  return json;
}