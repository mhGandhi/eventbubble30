public record ApiErrorResponse(
        String message,
        String path,
        Map<String, String> validationErrors,
        AuthState authState
) {}


das ist eine error response.
timestamp, code (4xx,5xx dies das) sind im Header

message -> noch was dazu. Wenn mit "E_" anfängt dann ist das maschinenlesbar und kann ausgewertet werden um Zeugs anzuzeigen.
validationErrors -> wenn irgendwelche Werte abgelehnt werden (falsche Länge, falsches Format, leerer Wert etc)

Zu message:
Oft noch einfache Texte, ändert sich nach und nach zu "E_[...]". Nur in dem Maschinenformat an Nutzer weiterleiten!!!
(Siehe com.lennadi.eventbubble30.exceptions.ErrorCodes.java)

Zu validation errors:
so werden die konstruiert:

Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err ->
                errors.put(err.getField(), err.getDefaultMessage())
        );

eif ausprobieren was zurückkommt oder so