# REST API do wymiany walut

## Wprowadzenie

Wymagania zadania:

Założenia funkcjonalne:
- Aplikacja posiada REST API pozwalające na założenie konta walutowego.
- Przy zakładaniu konta wymagane jest podanie początkowego salda konta w PLN.
- Aplikacja przy zakładaniu konta wymaga od użytkownika podania imienia i nazwiska.
- Aplikacja przy zakładaniu konta generuje identyfikator konta, który powinien być używany przy wywoływaniu dalszych metod API.
- Aplikacja powinna udostępnić REST API do wymiany pieniędzy w parze PLN<->USD (czyli PLN na USD oraz USD na PLN), a aktualny kurs wymiany pobrać z publicznego API NBP (http://api.nbp.pl/).
- Aplikacja powinna udostępnić REST API do pobrania danych o koncie i jego aktualnego stanu w PLN i USD.

Założenia niefunkcjonalne:
- Aplikacja musi zostać wykonana w Javie lub Kotlinie.
- Aplikacja może być wykonana w dowolnym frameworku.
- Aplikacja powinna zachowywać dane po restarcie.
- Kod źródłowy aplikacji powinien zostać udostępniony na wybranym portalu do hostowania kodu (np. Gitlab, Github, Bitbucket).
- Aplikacja musi być budowana przy pomocy narzędzia do budowania aplikacji (np. Maven, Gradle).
- Wymagane jest README z instrukcją pozwalającą uruchomić aplikację.
- W przypadku niesprecyzowania czegoś w zadaniu - pozostaje dowolność.
- W przypadku pytań – można się dopytywać mailowo.

## Technologie
* **Java 21** / **Spring Boot 3.x**
* **Gradle** (Build tool)
* **H2 Database** (In-memory z mechanizmem trwałości)
* **Resilience4j** (Circuit Breaker)
* **ArchUnit** (Automatyczna weryfikacja architektury)
* **Docker** (Konteneryzacja)

## Jak uruchomić?

### Opcja 1 — Docker (zalecane)

```bash
# Zbuduj obraz
docker build -t account-app .

# Uruchom kontener (dane zapisywane w wolumenie Docker)
docker run -p 8080:8080 -v account-data:/app/data account-app
```

### Opcja 2 — Gradle (lokalnie)

```bash
./gradlew bootRun
```

Aplikacja uruchomi się na porcie **8080**. Dane bazy H2 będą zapisywane lokalnie w katalogu `./data/`.

---

### Przydatne linki (po uruchomieniu)

| Zasób | URL |
|---|---|
| **Swagger UI** — interaktywna dokumentacja API | http://localhost:8080/swagger-ui.html |
| **OpenAPI JSON** — specyfikacja endpointów | http://localhost:8080/v3/api-docs |
| **H2 Console** — podgląd bazy danych | http://localhost:8080/h2-console |
| **NBP API** — zewnętrzne źródło kursów walut | https://api.nbp.pl |

> **H2 Console** — przy logowaniu użyj JDBC URL: `jdbc:h2:file:./data/account-db`, login: `sa`, hasło: _(puste)_.

---

## Decyzje projektowe

### Architektura i Design

Aplikacja została zaprojektowana zgodnie z zasadami **Architektury Heksagonalnej** (Ports & Adapters). Głównym celem było całkowite odseparowanie logiki biznesowej od szczegółów implementacyjnych infrastruktury. Dzięki temu system jest elastyczny, łatwy w testowaniu i odporny na zmiany zewnętrzne.

* **Separacja Modułów:** Logika biznesowa w module `account` komunikuje się z dostawcą kursów poprzez port `ExchangeRateProvider`. Jego konkretna implementacja (`NbpExchangeRateProvider`) znajduje się w module `exchange`. Takie podejście gwarantuje, że domena kont nie wie nic o API NBP, HttpClientach czy formacie odpowiedzi zewnętrznych serwisów.
* **Shared Kernel:** Wspólne elementy, takie jak klasa `Currency`, zostały wydzielone do pakietu `shared`. Zapewnia to spójność danych w całej aplikacji i eliminuje duplikację kodu, zachowując jednocześnie czystość zależności między modułami.
* **Rozszerzalność:** Obecna walidacja wymusza, aby przynajmniej jedna waluta w parze była PLN (zgodnie z wymaganiami zadania). Jednak dzięki zastosowanej abstrakcji, dodanie obsługi par czy innych dostawców kursów wymaga jedynie stworzenia nowego adaptera, bez modyfikacji jądra aplikacji. Konkretnie, aby odblokować np. wymianę USD↔EUR, wystarczą **4 zmiany**:
  1. **`Currency` (shared)** — dodać nową wartość do enuma, np. `EUR`. Dzięki `Map<Currency, BigDecimal>` w encji nowa waluta od razu pojawia się w saldach bez migracji schematu.
  2. **`ExchangeCurrencyUseCase`** — usunąć lub zastąpić guard `if (from != PLN && to != PLN)` logiką opartą na zbiorze obsługiwanych par, np. `SupportedCurrencyPairs.contains(from, to)`.
  3. **`NbpExchangeRateProvider.getRate()`** — dodać gałąź dla par bez PLN: kurs krzyżowy wyliczyć jako `getPlnRate(from) / getPlnRate(to)` (np. USD→EUR = 4.00 PLN/USD ÷ 4.20 PLN/EUR ≈ 0.952). Oba kursy pobierane są z tego samego cache'a NBP — infrastruktura nie wymaga żadnych zmian.
  4. **`NbpExchangeRateProvider.getPlnRate()`** — zastąpić jeden `ReentrantLock` mapą blokad per waluta (`ConcurrentHashMap<Currency, ReentrantLock>`), na co wskazuje istniejący komentarz w kodzie — eliminuje to zbędną rywalizację przy równoległym odświeżaniu różnych kursów.

### Skalowalność i Wizja Rozwoju

Projekt, mimo formy monolitu, jest przygotowany na scenariusze wysokiego obciążenia:
* **Mikroserwisy:** Moduły `account` i `exchange` mają na tyle luźne powiązania, że mogą zostać błyskawicznie wydzielone do osobnych usług komunikujących się przez REST.
* **Warstwa Danych:** Przejście z bazy H2 na rozwiązania produkcyjne (PostgreSQL, MySQL) wymaga jedynie zmiany konfiguracji Spring Data, bez dotykania logiki biznesowej.
* **Optymalizacja Odpowiedzi:** W celu skrócenia czasu odpowiedzi (latency), stan konta mógłby być cache'owany w **Redis**. Systemy kolejkowania (np. **Apache Kafka**) mogłyby służyć do budowania niemutowalnego dziennika zdarzeń (Event Sourcing), zapewniając pełną historię operacji oraz asynchroniczne zasilanie modeli analitycznych (CQRS), przy jednoczesnym zachowaniu bazy SQL jako głównego źródła prawdy.
* **Wirtualne watki**: Wykorzystanie Javy 21 pozwala na pełne odblokowanie potencjału aplikacji poprzez włączenie wirtualnych wątków (spring.threads.virtual.enabled=true). Dzięki temu operacje blokujące I/O (zapytania do API NBP czy dostęp do bazy danych) nie konsumują cennych wątków systemowych, co pozwala na niemal nieograniczone skalowanie liczby jednoczesnych połączeń przy minimalnym narzucie pamięciowym. Dzieki uzyciu ReetrantLock zamiast synchronized, unikamy problemu *thread starvation* i zapewniamy sprawiedliwy dostęp do sekcji krytycznych, nawet przy dużym obciążeniu. Aczkolwiek przy obecnej skali nie jest to wymagane, ale pokazuje gotowość aplikacji na przyszły wzrost.

---

### Kluczowe Rozwiązania Techniczne

* **Zarządzanie Stanem (JSON w SQL):** Salda kont przechowywane są w bazie H2 jako zserializowany obiekt JSON (`Map<Currency, BigDecimal>`). Eliminuje to potrzebę tworzenia sztywnych kolumn dla każdej waluty i pozwala na dynamiczne dodawanie nowych walut bez migracji schematu bazy danych.
* **Bezpieczeństwo Transakcji (Optimistic Locking):** Wykorzystanie mechanizmu `@Version` w encjach zapewnia integralność danych w środowisku wielowątkowym. Chroni to system przed problemem *Lost Update*, np. przy próbie wykonania kilku operacji na koncie w tym samym milisekundzie.
* **Strategia Kursów Walut (Consistency over Availability):**
    * **Cache On-demand:** Kursy są odświeżane co 3h w dni robocze. Odświeżanie następuje "leniwie" (lazy loading) podczas żądania użytkownika, co oszczędza zasoby i limity API NBP. W weekendy system automatycznie korzysta z ostatniego znanego kursu.
    * **Hard Limit (26h):** Bezpieczeństwo finansowe jest priorytetem. Jeśli dane z NBP są starsze niż 26h, system blokuje wymianę, aby uniknąć operowania na nieaktualnych kursach.
    * **Resilience:** Implementacja **Circuit Breaker** (Resilience4j) izoluje naszą aplikację od problemów wydajnościowych API NBP, zapobiegając zawieszaniu się wątków aplikacji.

---

### Quality Assurance (Zapewnienie Jakości)

* **ArchUnit:** Projekt posiada zautomatyzowane testy architektury. Pilnują one, aby żaden programista nie złamał zasad *Persistence Ignorance* (domena bez zależności od frameworków) oraz nie wprowadził niepożądanych zależności międzymodułowych.
* **Testy Integracyjne z WireMock:** Wszystkie scenariusze komunikacji z NBP (sukcesy, timeouty, błędy 500) są testowane przy użyciu atrap serwerowych. Gwarantuje to stabilność testów niezależnie od dostępności internetu.
* **Testy Jednostkowe:** Rdzeń matematyczny aplikacji (zaokrąglenia, walidacja sald) jest w 100% pokryty szybkimi testami jednostkowymi.