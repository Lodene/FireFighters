package com.simu.service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpResponse.BodyHandlers;

import org.assertj.core.internal.bytebuddy.matcher.CollectionSizeMatcher;

import com.fasterxml.jackson.databind.ObjectMapper;

import harness.endpoint.transfer.OutputStream;
import scala.collection.immutable.List;

import java.net.http.HttpResponse;
import java.net.http.HttpRequest;

public class Api {

    public static String sendGetRequest(String url) throws Exception {
        java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET() // C'est ici que l'on spécifie la méthode GET
                .build();

        java.net.http.HttpResponse<String> response = client.send(request,
                java.net.http.HttpResponse.BodyHandlers.ofString());

        return response.body();
    }

    public static String sendPostRequest(String url, String jsonInputString) throws Exception {
        java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonInputString))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return response.body();
    }

    public static String sendPutRequest(String url, String jsonInputString) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .PUT(HttpRequest.BodyPublishers.ofString(jsonInputString)) // Utilisation de la méthode PUT
                .header("Content-Type", "application/json") // Ajout de l'en-tête pour le type de contenu
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

}