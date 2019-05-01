package io.typesafe.caching;

interface Serializer<T> {

  String serialize(T value);

  T deserialize(String string);
}
