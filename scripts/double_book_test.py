#!/usr/bin/env python3
"""
Exercise the booking flow twice with the same slot and customer.

The app does not currently expose a JSON booking endpoint. Source code shows the
booking flow is:
  GET  /slots
  GET  /book?slotId=<id>
  POST /book with slotId, customerName, email, customerNotes
"""

from html.parser import HTMLParser
from http.cookiejar import CookieJar
from urllib.parse import parse_qs, urlencode, urljoin, urlparse
from urllib.request import HTTPCookieProcessor, Request, build_opener


BASE_URL = "http://localhost:8080"
CUSTOMER_NAME = "Alice Farmer"
CUSTOMER_EMAIL = "alice@farm.example.com"
CUSTOMER_NOTES = "Double-booking API test"


class BookingLinkParser(HTMLParser):
    def __init__(self):
        super().__init__()
        self.booking_links = []

    def handle_starttag(self, tag, attrs):
        if tag != "a":
            return
        attrs = dict(attrs)
        href = attrs.get("href", "")
        if href.startswith("/book?") or href.startswith("http://localhost:8080/book?"):
            self.booking_links.append(href)


class HiddenInputParser(HTMLParser):
    def __init__(self):
        super().__init__()
        self.hidden_inputs = {}

    def handle_starttag(self, tag, attrs):
        if tag != "input":
            return
        attrs = dict(attrs)
        if attrs.get("type") == "hidden" and attrs.get("name"):
            self.hidden_inputs[attrs["name"]] = attrs.get("value", "")


def fetch_text(opener, url):
    with opener.open(url) as response:
        return response.status, response.geturl(), response.read().decode("utf-8")


def first_available_slot_id(opener):
    status, final_url, html = fetch_text(opener, urljoin(BASE_URL, "/slots"))
    if status != 200:
        raise RuntimeError(f"GET /slots returned HTTP {status} at {final_url}")

    parser = BookingLinkParser()
    parser.feed(html)
    if not parser.booking_links:
        raise RuntimeError("No available booking links found on /slots")

    first_link = urljoin(BASE_URL, parser.booking_links[0])
    query = parse_qs(urlparse(first_link).query)
    try:
        return int(query["slotId"][0])
    except (KeyError, ValueError, IndexError) as exc:
        raise RuntimeError(f"Could not parse slotId from {first_link}") from exc


def csrf_fields_for_slot(opener, slot_id):
    status, final_url, html = fetch_text(opener, f"{BASE_URL}/book?slotId={slot_id}")
    if status != 200:
        raise RuntimeError(f"GET /book?slotId={slot_id} returned HTTP {status} at {final_url}")

    parser = HiddenInputParser()
    parser.feed(html)
    return {
        name: value
        for name, value in parser.hidden_inputs.items()
        if name not in {"slotId"}
    }


def submit_booking(opener, payload):
    data = urlencode(payload).encode("utf-8")
    request = Request(
        urljoin(BASE_URL, "/book"),
        data=data,
        headers={"Content-Type": "application/x-www-form-urlencoded"},
        method="POST",
    )
    with opener.open(request) as response:
        body = response.read().decode("utf-8", errors="replace")
        return response.status, response.geturl(), body


def main():
    opener = build_opener(HTTPCookieProcessor(CookieJar()))

    slot_id = first_available_slot_id(opener)
    payload = {
        "slotId": str(slot_id),
        "customerName": CUSTOMER_NAME,
        "email": CUSTOMER_EMAIL,
        "customerNotes": CUSTOMER_NOTES,
    }
    payload.update(csrf_fields_for_slot(opener, slot_id))

    print(f"Selected first available slotId={slot_id}")
    print(f"Booking customer={CUSTOMER_EMAIL}")

    for attempt in (1, 2):
        status, final_url, body = submit_booking(opener, payload)
        result = "accepted" if "/confirmation" in final_url else "rejected_or_redirected"
        print(f"Attempt {attempt}: HTTP {status}, final URL {final_url}, result={result}")
        if "Booking unavailable" in body or "already" in body.lower():
            print("  Server response includes an unavailable-slot message.")


if __name__ == "__main__":
    main()
