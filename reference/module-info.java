/**
 * Module configuration for Weather Information App
 * Defines dependencies and exports for the weather application
 *
 * NOTE: kept here for reference only. The active Maven build in this
 * project runs in classpath mode (no module-info.java under src/main/java)
 * because it is far more reliable with the javafx-maven-plugin + org.json's
 * automatic module. Drop this file into src/main/java/ if you want a JPMS
 * modular build instead.
 */
module weatherapp {
    // JavaFX dependencies
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.base;
    requires javafx.graphics;

    // JSON processing
    requires org.json;

    // Java standard libraries
    requires java.net.http;
    requires java.desktop;

    // Export main package
    exports weatherapp;
}
