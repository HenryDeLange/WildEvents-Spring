package mywild.core.rest;

import java.util.List;

public record Paged<T>(int page, long total, List<T> data) {
}
