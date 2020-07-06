package dev.pgm.community.reports;

import dev.pgm.community.feature.Feature;
import java.util.concurrent.CompletableFuture;

public interface ReportFeature extends Feature {

  CompletableFuture<Report> report();
}
