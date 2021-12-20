module com.udacity.security {
    opens com.udacity.security.data to com.google.gson;
    requires com.udacity.image;
    requires java.desktop;
    requires miglayout;
    requires com.google.gson;
    requires com.google.common;
    requires java.prefs;

}