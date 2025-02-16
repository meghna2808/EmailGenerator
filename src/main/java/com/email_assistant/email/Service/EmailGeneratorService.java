package com.email_assistant.email.Service;

import com.email_assistant.email.EmailRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class EmailGeneratorService {
    @Value("${gemini.api.url}")
    private String geminiApiUrl;
    @Value("${gemini.api.key}")
    private String geminiApiKey;
    private WebClient webClient;
    public EmailGeneratorService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }


     public String generateEmailReply(EmailRequest emailRequest)
     {
         // generate a prompt;
         String prompt=buildPrompt(emailRequest);
         // craft request
         Map<String, Object> requestBody=Map.of("contents",new Object[]{
                 Map.of("parts", new Object[]{
                         Map.of("text",prompt)
                 })
         });
         //send request  and get response using webflux
         Mono<String> responseMono =webClient.post()
                 .uri(geminiApiUrl+geminiApiKey)
                 .header("Content-type","application/json")
                 .bodyValue(requestBody)
                 .retrieve()
                 .bodyToMono(String.class);
         String response=responseMono.block();

         //extract response and return
         return extractResponse(response);
     }

    private String extractResponse(String response) {
        try{
            ObjectMapper mapper=new ObjectMapper();
            JsonNode root=mapper.readTree(response);
            return root.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();
        }
        catch (Exception e)
        {
            return "Error processing request" +e.getMessage();
        }

    }

    String buildPrompt(EmailRequest emailRequest)
     {
         StringBuilder prompt=new StringBuilder();
         prompt.append("Generate a professional email reply for the following email content. Don't generate subject. Only provide one best response. Don't provide additional things other than email content");
         if(emailRequest.getTone()!=null && !emailRequest.getTone().isEmpty())
         {
             prompt.append("Use a").append(emailRequest.getTone()).append("tone.");
         }
         prompt.append("Original Email:\n").append(emailRequest.getEmailContent());
         return prompt.toString();
     }
}
