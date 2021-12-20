module com.udacity.security {
    opens com.udacity.security.data to com.google.gson;
    requires com.udacity.image;
    requires miglayout;
    requires java.desktop;
    requires com.google.gson;
    requires com.google.common;
    requires java.prefs;

}