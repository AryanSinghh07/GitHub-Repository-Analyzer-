import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.*;

public class GitHubCodeAnalyser {

    private static final String USER_DATABASE = "users.csv";
    private static final String HISTORY_DATABASE = "history.csv";
    private static final String REPORTS_DATABASE = "reports.csv";
    private static final String COMMENTS_DATABASE = "comments.csv";
    private static final String CODE_HISTORY_DATABASE = "code_history.csv";
    private static final Map<String, String> users = new HashMap<>();
    private static JFrame mainFrame;
    private static String currentUser;

    public static void main(String[] args) {
        loadUsers();
        SwingUtilities.invokeLater(GitHubCodeAnalyser::showLoginScreen);
    }

    private static void loadUsers() {
        try (BufferedReader reader = new BufferedReader(new FileReader(USER_DATABASE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    users.put(parts[0], parts[1]);
                }
            }
        } catch (IOException e) {
            System.out.println("User  database not found or could not be read.");
        }
    }

    private static void saveUsers() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(USER_DATABASE))) {
            for (Map.Entry<String, String> entry : users.entrySet()) {
                writer.write(entry.getKey() + "," + entry.getValue());
                writer.newLine();
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(mainFrame, "Error saving user data!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void logHistory(String action, String details) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(HISTORY_DATABASE, true))) {
            writer.write(currentUser + "," + LocalDateTime.now() + "," + action + "," + details);
            writer.newLine();
        } catch (IOException e) {
            System.out.println("Error logging history.");
        }
    }

    private static void logCodeHistory(String changeType, String changeDetails, String filePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(CODE_HISTORY_DATABASE, true))) {
            writer.write(currentUser + "," + filePath + "," + changeType + "," + changeDetails + "," + LocalDateTime.now());
            writer.newLine();
        } catch (IOException e) {
            System.out.println("Error logging code history.");
        }
    }

    private static void showLoginScreen() {
        mainFrame = new JFrame("GitHub Code Analyser - Login");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(400, 300);

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(4, 2, 10, 10));

        JLabel usernameLabel = new JLabel("Username:");
        JTextField usernameField = new JTextField();

        JLabel passwordLabel = new JLabel("Password:");
        JPasswordField passwordField = new JPasswordField();

        JButton loginButton = new JButton("Login");
        JButton registerButton = new JButton("Register");

        loginButton.addActionListener(e -> handleLogin(usernameField.getText(), new String(passwordField.getPassword())));
        registerButton.addActionListener(e -> handleRegister(usernameField.getText(), new String(passwordField.getPassword())));

        panel.add(usernameLabel);
        panel.add(usernameField);
        panel.add(passwordLabel);
        panel.add(passwordField);
        panel.add(loginButton);
        panel.add(registerButton);

        mainFrame.add(panel);
        mainFrame.setVisible(true);
    }

    private static void handleLogin(String username, String password) {
        if (users.containsKey(username) && users.get(username).equals(password)) {
            currentUser = username;
            logHistory("Login", "User  logged in.");
            JOptionPane.showMessageDialog(mainFrame, "Login successful!");
            showRepositoryManagerScreen();
        } else {
            JOptionPane.showMessageDialog(mainFrame, "Invalid credentials!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void handleRegister(String username, String password) {
        if (users.containsKey(username)) {
            JOptionPane.showMessageDialog(mainFrame, "Username already exists!", "Error", JOptionPane.ERROR_MESSAGE);
        } else {
            users.put(username, password);
            saveUsers();
            JOptionPane.showMessageDialog(mainFrame, "Registration successful!");
        }
    }

    private static void showRepositoryManagerScreen() {
        mainFrame.getContentPane().removeAll();
        mainFrame.setTitle("GitHub Code Analyser");
        mainFrame.setSize(600, 400);

        JPanel panel = new JPanel(new BorderLayout());

        JButton uploadButton = new JButton("Upload Repository");
        JButton historyButton = new JButton("Check History");
        JButton shareReportButton = new JButton("Share Report");
        JButton createFileButton = new JButton("Create New File");
        JButton viewCodeHistoryButton = new JButton("View Code History");
        DefaultListModel<String> fileListModel = new DefaultListModel<>();
        JList<String> fileList = new JList<>(fileListModel);
        JTextArea fileViewer = new JTextArea();
        fileViewer.setEditable(true);

        JButton analyzeButton = new JButton("Analyze File");
        JButton saveButton = new JButton("Save File");
        JButton searchButton = new JButton("Search in File");
        JButton commentButton = new JButton("Comment on Line");

        uploadButton.addActionListener(e -> handleRepositoryUpload(fileListModel));
        historyButton.addActionListener(e -> showHistory());
        shareReportButton.addActionListener(e -> shareReport());
        createFileButton.addActionListener(e -> createNewFile(fileListModel, fileViewer));
        viewCodeHistoryButton.addActionListener(e -> showCodeHistory());
        fileList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedFile = fileList.getSelectedValue();
                if (selectedFile != null) {
                    displayFileContent(selectedFile, fileViewer);
                }
            }
        });

        analyzeButton.addActionListener(e -> {
            String selectedFile = fileList.getSelectedValue();
            if (selectedFile != null) {
                analyzeFile(selectedFile);
            } else {
                JOptionPane.showMessageDialog(mainFrame, "Please select a file to analyze!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        saveButton.addActionListener(e -> {
            String selectedFile = fileList.getSelectedValue();
            if (selectedFile != null) {
                saveFileContent(selectedFile, fileViewer);
                logHistory("File Saved", selectedFile);
            } else {
                JOptionPane.showMessageDialog(mainFrame, "Please select a file to save!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        searchButton.addActionListener(e -> {
            String selectedFile = fileList.getSelectedValue();
            if (selectedFile != null) {
                String searchTerm = JOptionPane.showInputDialog(mainFrame, "Enter term to search:");
                if (searchTerm != null && !searchTerm.isEmpty()) {
                    searchInFile(selectedFile, searchTerm);
                    logHistory("Search Performed", "Term: " + searchTerm);
                }
            } else {
                JOptionPane.showMessageDialog(mainFrame, "Please select a file to search!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        commentButton.addActionListener(e -> {
            String selectedFile = fileList.getSelectedValue();
            if (selectedFile != null) {
                String[] fileTypes = {"Java", "Python", "HTML", "CSS", "JavaScript"};
                String fileType = (String) JOptionPane.showInputDialog(mainFrame, "Select file type:", "Select File Type",
                        JOptionPane.PLAIN_MESSAGE, null, fileTypes, fileTypes[0]);

                if (fileType != null) {
                    String[] commentTypes = {"Single Line", "Multi Line", "TODO", "FIXME"};
                    String commentType = (String) JOptionPane.showInputDialog(mainFrame, "Select comment type:", "Select Comment Type",
                            JOptionPane.PLAIN_MESSAGE, null, commentTypes, commentTypes[0]);

                    if (commentType != null) {
                        String lineNumberStr = JOptionPane.showInputDialog(mainFrame, "Enter line number to comment:");
                        if (lineNumberStr != null && !lineNumberStr.isEmpty()) {
                            try {
                                int lineNumber = Integer.parseInt(lineNumberStr);
                                String comment = "";
                                if (commentType.equals("TODO")) {
                                    comment = JOptionPane.showInputDialog(mainFrame, "Enter TODO comment:");
                                    comment = "TODO: " + comment;
                                } else if (commentType.equals("FIXME")) {
                                    comment = JOptionPane.showInputDialog(mainFrame, "Enter FIXME comment:");
                                    comment = "FIXME: " + comment;
                                } else {
                                    comment = JOptionPane.showInputDialog(mainFrame, "Enter your comment:");
                                }
                                if (comment != null && !comment.isEmpty()) {
                                    saveComment(selectedFile, lineNumber, comment);
                                    logHistory("Comment Added", "File: " + selectedFile + ", Line: " + lineNumber);

                                    // Display the comment in the file viewer
                                    java.util.List<String> lines = new ArrayList<>(Arrays.asList(fileViewer.getText().split("\n")));
                                    if (lineNumber > 0 && lineNumber <= lines.size()) {
                                        String formattedComment = formatComment(fileType, commentType, comment);
                                        lines.add(lineNumber - 1, formattedComment);
                                        fileViewer.setText(String.join("\n", lines));
                                        saveFileContent(selectedFile, fileViewer); // Save the file with the new comment
                                    } else {
                                        JOptionPane.showMessageDialog(mainFrame, "Line number out of range!", "Error", JOptionPane.ERROR_MESSAGE);
                                    }
                                }
                            } catch (NumberFormatException ex) {
                                JOptionPane.showMessageDialog(mainFrame, "Invalid line number!", "Error", JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }
                }
            } else {
                JOptionPane.showMessageDialog(mainFrame, "Please select a file to comment on!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(uploadButton);
        buttonPanel.add(shareReportButton);
        buttonPanel.add(createFileButton);
        buttonPanel.add(viewCodeHistoryButton);
        buttonPanel.add(analyzeButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(searchButton);
        buttonPanel.add(commentButton);

        panel.add(new JScrollPane(fileList), BorderLayout.WEST);
        panel.add(new JScrollPane(fileViewer), BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        mainFrame.add(panel);
        mainFrame.revalidate();
        mainFrame.repaint();
    }

    private static void handleRepositoryUpload(DefaultListModel<String> fileListModel) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int returnValue = fileChooser.showOpenDialog(mainFrame);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFolder = fileChooser.getSelectedFile();
            fileListModel.clear();

            try {
                Files.walk(selectedFolder.toPath())
                        .filter(Files::isRegularFile)
                        .forEach(filePath -> fileListModel.addElement(filePath.toString()));

                logHistory("Repository Uploaded", selectedFolder.getAbsolutePath());
                JOptionPane.showMessageDialog(mainFrame, "Repository uploaded successfully!");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(mainFrame, "Error reading files from the folder!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static void displayFileContent(String filePath, JTextArea fileViewer) {
        try {
            java.util.List<String> lines = Files.readAllLines(Paths.get(filePath));
            StringBuilder content = new StringBuilder();
            for (String line : lines) {
                content.append(line).append("\n");
            }
            fileViewer.setText(content.toString());
        } catch (IOException e) {
            fileViewer.setText("Error reading file: " + filePath);
        }
    }

    private static void analyzeFile(String filePath) {
        try {
            java.util.List<String> lines = Files.readAllLines(Paths.get(filePath));
            int lineCount = lines.size();
            int blankLineCount = 0;
            int commentLineCount = 0;
            int todoCount = 0;
            int fixmeCount = 0;

            String fileType = filePath.substring(filePath.lastIndexOf('.') + 1);
            String commentPattern = getCommentPattern(fileType);

            StringBuilder commentReport = new StringBuilder();

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                String trimmedLine = line.trim();
                if (trimmedLine.isEmpty()) {
                    blankLineCount++;
                } else if (trimmedLine.startsWith("//") || trimmedLine.startsWith("#") || trimmedLine.startsWith("/*") || trimmedLine.startsWith("<!--")) {
                    commentLineCount++;
                    if (trimmedLine.contains("TODO")) {
                        todoCount++;
                        commentReport.append("TODO: ").append(trimmedLine).append(" (Line ").append(i + 1).append(")\n");
                    }
                    if (trimmedLine.contains("FIXME")) {
                        fixmeCount++;
                        commentReport.append("FIXME: ").append(trimmedLine).append(" (Line ").append(i + 1).append(")\n");
                    }
                }
            }

            String report = "File Type: " + fileType + "\nLines of Code: " + lineCount +
                    "\nBlank Lines: " + blankLineCount + "\nComment Lines: " + commentLineCount +
                    "\nTODOs: " + todoCount + "\nFIXMEs: " + fixmeCount + "\n\nComment Report:\n" + commentReport.toString();

            JOptionPane.showMessageDialog(mainFrame, report, "File Analysis", JOptionPane.INFORMATION_MESSAGE);
            saveReport(filePath, report);
            logHistory("File Analyzed", filePath);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(mainFrame, "Error analyzing file: " + filePath, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String getCommentPattern(String fileType) {
        switch (fileType) {
            case "java":
            case "js":
            case "c":
            case "cpp":
                return "^\\s*(//|/\\*|\\*|\\*/)"; // Java, JavaScript, C, C++
            case "py":
                return "^\\s*(#|\"\"\"|''')"; // Python
            case "html":
            case "xml":
                return "^\\s*<!--|-->"; // HTML, XML
            case "css":
                return "^\\s*/\\*|\\*/"; // CSS
            default:
                return "^\\s*#"; // Default to hash comments
        }
    }

    private static void saveFileContent(String filePath, JTextArea fileViewer) {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(filePath))) {
            writer.write(fileViewer.getText());
            JOptionPane.showMessageDialog(mainFrame, "File saved successfully!");
            logCodeHistory("modified", "Modified file content", filePath);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(mainFrame, "Error saving file: " + filePath, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void searchInFile(String filePath, String searchTerm) {
        try {
            java.util.List<String> lines = Files.readAllLines(Paths.get(filePath));
            int totalOccurrences = 0;
            StringBuilder result = new StringBuilder("Search results for '" + searchTerm + "':\n");

            Pattern pattern = Pattern.compile(Pattern.quote(searchTerm));

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                Matcher matcher = pattern.matcher(line);
                int occurrencesInLine = 0;
                while (matcher.find()) {
                    occurrencesInLine++;
                }

                if (occurrencesInLine > 0) {
                    result.append("Line ").append(i + 1).append(": ").append(occurrencesInLine).append(" occurrence(s)\n");
                    totalOccurrences += occurrencesInLine;
                }
            }

            if (totalOccurrences > 0) {
                result.append("\nTotal occurrences: ").append(totalOccurrences);
                JOptionPane.showMessageDialog(mainFrame, result.toString(), "Search Results", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(mainFrame, "No occurrences found for '" + searchTerm + "'.", "Search Results", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(mainFrame, "Error searching file: " + filePath, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void showHistory() {
        JFrame historyFrame = new JFrame("User  History");
        historyFrame.setSize(600, 400);

        JTextArea historyArea = new JTextArea();
        historyArea.setEditable(false);

        try (BufferedReader reader = new BufferedReader(new FileReader(HISTORY_DATABASE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", 4);
                if (parts.length == 4 && parts[0].equals(currentUser)) {
                    historyArea.append("Date: " + parts[1] + "\nAction: " + parts[2] + "\nDetails: " + parts[3] + "\n\n");
                }
            }
        } catch (IOException e) {
            historyArea.setText("No history available or error reading history.");
        }

        historyFrame.add(new JScrollPane(historyArea));
        historyFrame.setVisible(true);
    }

    private static void saveReport(String filePath, String report) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(REPORTS_DATABASE, true))) {
            writer.write(currentUser + "," + filePath + "," + report.replace("\n", "\\n"));
            writer.newLine();
        } catch (IOException e) {
            System.out.println("Error saving report.");
        }
    }

    private static void shareReport() {
        JFrame shareFrame = new JFrame("Share Analysis Reports");
        shareFrame.setSize(600, 400);

        JTextArea reportArea = new JTextArea();
        reportArea.setEditable(false);

        try (BufferedReader reader = new BufferedReader(new FileReader(REPORTS_DATABASE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", 3);
                if (parts.length == 3) {
                    reportArea.append(":User  " + parts[0] + "\nFile: " + parts[1] + "\nReport: " + parts[2].replace("\\n", "\n") + "\n\n");
                }
            }
        } catch (IOException e) {
            reportArea.setText("No reports available or error reading reports.");
        }

        shareFrame.add(new JScrollPane(reportArea));
        shareFrame.setVisible(true);
    }

    private static void saveComment(String filePath, int lineNumber, String comment) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(COMMENTS_DATABASE, true))) {
            writer.write(currentUser + "," + filePath + "," + lineNumber + "," + comment.replace("\n", "\\n"));
            writer.newLine();
        } catch (IOException e) {
            System.out.println("Error saving comment.");
        }
    }

    private static String formatComment(String fileType, String commentType, String comment) {
        switch (fileType) {
            case "Java":
            case "JavaScript":
            case "C":
            case "C++":
                if (commentType.equals("TODO")) {
                    return "// TODO: " + comment;
                } else if (commentType.equals("FIXME")) {
                    return "// FIXME: " + comment;
                } else if (commentType.equals("Single Line")) {
                    return "// " + comment;
                } else {
                    return "/* " + comment + " */";
                }
            case "Python":
                if (commentType.equals("TODO")) {
                    return "# TODO: " + comment;
                } else if (commentType.equals("FIXME")) {
                    return "# FIXME: " + comment;
                } else if (commentType.equals("Single Line")) {
                    return "# " + comment;
                } else {
                    return "\"\"\"\n" + comment + "\n\"\"\"";
                }
            case "HTML":
            case "XML":
                if (commentType.equals("TODO")) {
                    return "<!-- TODO: " + comment + " -->";
                } else if (commentType.equals("FIXME")) {
                    return "<!-- FIXME: " + comment + " -->";
                } else if (commentType.equals("Single Line")) {
                    return "<!-- " + comment + " -->";
                } else {
                    return "<!--\n" + comment + "\n-->";
                }
            case "CSS":
                if (commentType.equals("TODO")) {
                    return "/* TODO: " + comment + " */";
                } else if (commentType.equals("FIXME")) {
                    return "/* FIXME: " + comment + " */";
                } else if (commentType.equals("Single Line")) {
                    return "/* " + comment + " */";
                } else {
                    return "/*\n" + comment + "\n*/";
                }
            default:
                throw new UnsupportedOperationException("Unsupported file type: " + fileType);
        }
    }

    private static void createNewFile(DefaultListModel<String> fileListModel, JTextArea fileViewer) {
        String[] fileTypes = {"Java", "Python", "HTML", "CSS", "JavaScript"};
        String fileType = (String) JOptionPane.showInputDialog(mainFrame, "Select file type:", "Create New File",
                JOptionPane.PLAIN_MESSAGE, null, fileTypes, fileTypes[0]);

        if (fileType != null) {
            String fileName = JOptionPane.showInputDialog(mainFrame, "Enter file name (without extension):");
            if (fileName != null && !fileName.trim().isEmpty()) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

                int returnValue = fileChooser.showSaveDialog(mainFrame);
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    File selectedFolder = fileChooser.getSelectedFile();
                    String extension = getFileExtension(fileType);
                    File newFile = new File(selectedFolder, fileName + extension);
                    try {
                        if (newFile.createNewFile()) {
                            JOptionPane.showMessageDialog(mainFrame, "File created successfully: " + newFile.getName());
                            logHistory("File Created", newFile.getName());
                            fileListModel.addElement(newFile.getAbsolutePath());
                            displayFileContent(newFile.getAbsolutePath(), fileViewer);
                        } else {
                            JOptionPane.showMessageDialog(mainFrame, "File already exists!", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (IOException e) {
                        JOptionPane.showMessageDialog(mainFrame, "Error creating file: " + newFile.getName(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } else {
                JOptionPane.showMessageDialog(mainFrame, "File name cannot be empty!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static String getFileExtension(String fileType) {
        switch (fileType) {
            case "Java":
                return ".java";
            case "Python":
                return ".py";
            case "HTML":
                return ".html";
            case "CSS":
                return ".css";
            case "JavaScript":
                return ".js";
            case "C":
                return ".c";
            default:
                return "";
        }
    }

    private static void showCodeHistory() {
        JFrame codeHistoryFrame = new JFrame("Code History");
        codeHistoryFrame.setSize(600, 400);

        JTextArea codeHistoryArea = new JTextArea();
        codeHistoryArea.setEditable(false);

        try (BufferedReader reader = new BufferedReader(new FileReader(CODE_HISTORY_DATABASE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", 5);
                if (parts.length == 5) {
                    codeHistoryArea.append(":User  " + parts[0] + "\nFile: " + parts[1] + "\nChange Type: " + parts[2] + "\nChange Details: " + parts[3] + "\nTimestamp: " + parts[4] + "\n\n");
                }
            }
        } catch (IOException e) {
            codeHistoryArea.setText("No code history available or error reading code history.");
        }

        codeHistoryFrame.add(new JScrollPane(codeHistoryArea));
        codeHistoryFrame.setVisible(true);
    }
}