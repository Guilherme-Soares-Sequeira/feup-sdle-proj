package org.C2.utils;

public interface JsonSerializable<T> {
    public T fromJson(String json);
    public String toJson();
}
