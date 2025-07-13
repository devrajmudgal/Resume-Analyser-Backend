package com.example.ResumeAnalyzer.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ResumeService {

    // Removed OpenAI API key (no longer needed)

    public Map<String, Object> analyzeResume(MultipartFile file, String jobDesc) {
        Map<String, Object> result = new LinkedHashMap<>();
        String resumeText = null;

        try {
            resumeText = extractTextFromPdf(file);
            Set<String> jdKeywords = extractKeywords(jobDesc);
            Set<String> resumeKeywords = extractKeywords(resumeText);

            Set<String> matched = new HashSet<>(resumeKeywords);
            matched.retainAll(jdKeywords);

            Set<String> missing = new HashSet<>(jdKeywords);
            missing.removeAll(resumeKeywords);

            int score = (int) (((double) matched.size() / jdKeywords.size()) * 100);

            List<String> suggestions = missing.stream()
                    .map(skill -> "Consider adding relevant experience or mention of \"" + skill + "\"")
                    .collect(Collectors.toList());

            result.put("matchScore", score + "%");
            result.put("matchedSkills", matched);
            result.put("missingSkills", missing);
            result.put("suggestions", suggestions);

            // 🔥 Add AI-based suggestions
            String aiTips = generateMockSuggestions(resumeText, jobDesc);
            result.put("aiSuggestions", aiTips);

        } catch (Exception e) {
            result.put("error", "Failed to process file: " + e.getMessage());
        }

        return result;
    }

    private String extractTextFromPdf(MultipartFile file) throws IOException {
        PDDocument document = PDDocument.load(file.getInputStream());
        PDFTextStripper pdfStripper = new PDFTextStripper();
        String text = pdfStripper.getText(document);
        document.close();
        return text;
    }

    private Set<String> extractKeywords(String text) {
        return Arrays.stream(text.toLowerCase().split("[\\W_]+"))
                .filter(word -> word.length() > 2)  // avoid short words like "a", "is", "to"
                .collect(Collectors.toSet());
    }

    private String generateMockSuggestions(String resumeText, String jobDesc) {
        List<String> tips = new ArrayList<>();

        if (resumeText.length() < 500) {
            tips.add("📝 Your resume seems too short. Add more detailed experiences or projects.");
        }
        if (!resumeText.toLowerCase().contains("project")) {
            tips.add("💡 Mention at least one project to showcase your practical skills.");
        }
        if (!resumeText.toLowerCase().contains("achievement")) {
            tips.add("🏆 Highlight notable achievements to stand out.");
        }
        if (!resumeText.toLowerCase().contains("github")) {
            tips.add("🔗 Include a GitHub or portfolio link to showcase your work.");
        }
        if (jobDesc.toLowerCase().contains("team") && !resumeText.toLowerCase().contains("team")) {
            tips.add("👥 Emphasize your ability to work in teams — it's mentioned in the job description.");
        }
        if (jobDesc.toLowerCase().contains("communication") && !resumeText.toLowerCase().contains("communication")) {
            tips.add("🗣 Highlight your communication skills if you have any relevant examples.");
        }

        if (tips.isEmpty()) {
            tips.add("✅ Your resume looks solid for this job. Just make sure it's tailored.");
        }

        return String.join("\n", tips);
    }

}
