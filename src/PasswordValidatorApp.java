import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * Aplicación de validación de contraseñas que verifica si una contraseña dada
 * cumple con ciertos criterios utilizando expresiones regulares, procesa la validación
 * de manera concurrente utilizando un ExecutorService, y registra los resultados en un archivo.
 * Este programa extiende la funcionalidad de la Actividad #9 al añadir el registro de resultados
 * y utiliza expresiones Lambda y manejo de archivos.
 */
public class PasswordValidatorApp {

    // Patrones de expresiones regulares para los criterios de validación de contraseña.
    // Verifica si la contraseña tiene al menos 8 caracteres de longitud.
    static final Pattern LENGTH = Pattern.compile(".{8,}");
    // Verifica si la contraseña contiene al menos un carácter especial (no es una letra o dígito).
    static final Pattern SPECIAL = Pattern.compile(".*[^a-zA-Z0-9].*");
    // Verifica si la contraseña contiene al menos dos letras mayúsculas.
    static final Pattern UPPER = Pattern.compile(".*[A-Z].*[A-Z].*");
    // Verifica si la contraseña contiene al menos tres letras minúsculas.
    static final Pattern LOWER = Pattern.compile(".*[a-z].*[a-z].*[a-z].*");
    // Verifica si la contraseña contiene al menos un dígito.
    static final Pattern DIGIT = Pattern.compile(".*\\d.*");

    // Nombre del archivo de registro donde se guardarán los resultados de la validación.
    static final String LOG_FILE = "registro.txt";

    /**
     * Método principal para ejecutar la aplicación de validación de contraseñas con registro.
     * Inicializa un Scanner para la entrada del usuario y un ExecutorService para la validación concurrente.
     * Solicita continuamente al usuario contraseñas para validar hasta que se escribe 'exit'.
     * La validación y el registro se realizan de forma asíncrona utilizando un pool de hilos.
     * @param args Argumentos de línea de comandos (no utilizados).
     */
    public static void main(String[] args) {
        // Inicializa un objeto Scanner para leer la entrada del usuario desde la consola.
        Scanner scanner = new Scanner(System.in);
        // Crea un pool de hilos fijo para ejecutar tareas de validación y registro de forma concurrente.
        ExecutorService executor = Executors.newFixedThreadPool(4);

        System.out.println("\n=== Validador de Contraseñas (con registro) ===");
        System.out.println("Escribe 'exit' para terminar.\n");

        // Bucle principal para leer contraseñas del usuario.
        while (true) {
            System.out.print("Contraseña: ");
            String input = scanner.nextLine();
            // Sale del bucle si el usuario escribe 'exit' (sin distinguir mayúsculas/minúsculas).
            if (input.equalsIgnoreCase("exit")) break;

            // Envía la tarea de validación y registro al servicio de ejecución para su ejecución asíncrona.
            // Se utiliza una expresión Lambda para definir la tarea a ejecutar.
            // La variable input es efectivamente final aquí.
            executor.execute(() -> {
                // Valida la contraseña y obtiene el estado del resultado y la retroalimentación detallada.
                ValidationResult validationResult = validate(input);
                // Imprime el estado de la validación y la contraseña en la consola.
                System.out.printf(" [%s] \"%s\"", validationResult.getStatus(), input);

                // Si la contraseña es inválida, imprime la retroalimentación detallada en la consola.
                if (!validationResult.isValid()) {
                    System.out.println("\n   La validación falló:");
                    System.out.print(validationResult.getFeedback()); // Usa print en lugar de println para evitar una nueva línea extra.
                } else {
                    System.out.println(); // Imprime una nueva línea para contraseñas válidas para un espaciado de salida consistente.
                }

                // Registra el resultado de la validación en el archivo de registro.
                logResult(input, validationResult.getStatus());
            });
        }

        // Apaga el servicio de ejecución, permitiendo que las tareas actualmente en ejecución se completen de forma segura.
        executor.shutdown();
        // Cierra el recurso Scanner para prevenir fugas de recursos.
        scanner.close();
    }

    /**
     * Valida una contraseña dada contra criterios predefinidos utilizando patrones de expresiones regulares.
     * Retorna un objeto ValidationResult que contiene el estado de la validación y retroalimentación detallada.
     * @param pass La cadena de la contraseña a validar.
     * @return Un objeto ValidationResult con el estado de la validación y retroalimentación.
     */
    static ValidationResult validate(String pass) {
        boolean isValid = true;
        StringBuilder feedback = new StringBuilder();

        // Verifica cada criterio de validación y añade retroalimentación si falla.
        if (!LENGTH.matcher(pass).matches()) {
            isValid = false;
            feedback.append(" - Debe tener al menos 8 caracteres de longitud.\n");
        }
        if (!SPECIAL.matcher(pass).matches()) {
            isValid = false;
            feedback.append(" - Debe contener al menos un carácter especial.\n");
        }
        if (!UPPER.matcher(pass).matches()) {
            isValid = false;
            feedback.append(" - Debe contener al menos dos letras mayúsculas.\n");
        }
        if (!LOWER.matcher(pass).matches()) {
            isValid = false;
            feedback.append(" - Debe contener al menos tres letras minúsculas.\n");
        }
        if (!DIGIT.matcher(pass).matches()) {
            isValid = false;
            feedback.append(" - Debe contener al menos un dígito.\n");
        }

        // Determina la cadena del estado general de la validación.
        String resultStatus = isValid ? "✔ VÁLIDA" : "✖ INVÁLIDA";

        return new ValidationResult(isValid, resultStatus, feedback.toString());
    }

    /**
     * Registra el resultado de la validación de una contraseña en el archivo de registro especificado.
     * Utiliza la palabra clave synchronized para asegurar la escritura segura en el archivo por múltiples hilos.
     * @param pass La contraseña que fue validada.
     * @param result El estado del resultado de la validación (ej. "✔ VÁLIDA", "✖ INVÁLIDA").
     */
    static synchronized void logResult(String pass, String result) {
        // Usa try-with-resources para asegurar que PrintWriter y FileWriter se cierren automáticamente.
        try (PrintWriter out = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            // Añade el resultado de la validación y la contraseña al archivo de registro.
            out.printf("[%s] \"%s\"\n", result, pass);
        } catch (IOException e) {
            // Imprime un mensaje de error en el flujo de error estándar si el registro falla.
            System.err.println("Error escribiendo al archivo de registro: " + e.getMessage());
        }
    }

    /**
     * Clase auxiliar para contener el resultado de la validación, la cadena de estado y la retroalimentación.
     */
    static class ValidationResult {
        private final boolean isValid;
        private final String status;
        private final String feedback;

        /**
         * Construye un objeto ValidationResult.
         * @param isValid Booleano que indica si la contraseña es válida.
         * @param status La cadena de estado (ej. "✔ VÁLIDA", "✖ INVÁLIDA").
         * @param feedback Retroalimentación detallada sobre los fallos de validación.
         */
        public ValidationResult(boolean isValid, String status, String feedback) {
            this.isValid = isValid;
            this.status = status;
            this.feedback = feedback;
        }

        /**
         * Retorna si la contraseña es válida.
         * @return true si es válida, false en caso contrario.
         */
        public boolean isValid() {
            return isValid;
        }

        /**
         * Retorna la cadena del estado de la validación.
         * @return La cadena de estado.
         */
        public String getStatus() {
            return status;
        }

        /**
         * Retorna la retroalimentación detallada sobre los fallos de validación.
         * @return La cadena de retroalimentación.
         */
        public String getFeedback() {
            return feedback;
        }
    }
}