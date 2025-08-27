open module org.hyperskill.academy.format {
  requires com.fasterxml.jackson.core;
  requires com.fasterxml.jackson.databind;
  requires com.fasterxml.jackson.annotation;
  requires com.fasterxml.jackson.dataformat.yaml;
  requires com.fasterxml.jackson.datatype.jsr310;
  requires com.fasterxml.jackson.kotlin;
  requires okhttp3;
  requires okhttp3.logging;
  requires retrofit2;
  requires retrofit2.converter.jackson;
  requires java.logging;
  requires kotlin.stdlib;
  requires org.jetbrains.annotations;

  exports org.hyperskill.academy.learning.courseFormat;
  exports org.hyperskill.academy.learning.courseFormat.tasks;
  exports org.hyperskill.academy.learning;
  exports org.hyperskill.academy.learning.json;
  exports org.hyperskill.academy.learning.json.mixins;
  exports org.hyperskill.academy.learning.yaml;
  exports org.hyperskill.academy.learning.yaml.errorHandling;
  exports org.hyperskill.academy.learning.yaml.format;
}
