import org.json.JSONObject;

record Config(String jfrFile, String memory, String cpu, String framework) {}

void main(String[] args) {
    if (args.length == 0 || args[0].equals("--help") || args[0].equals("-h")) {
        System.out.print(usage());
        return;
    }

    String cmd = args[0];
    if (cmd.equals("debug-tool")) {
        runDebugTool(args);
        return;
    }
    if (!cmd.equals("analyze")) {
        System.err.println("Error: unknown command '" + cmd + "'");
        System.err.print(usage());
        System.exit(1);
    }

    if (args.length < 2) {
        System.err.println("Error: 'analyze' requires a <jfr-file> argument");
        System.err.print(usage());
        System.exit(1);
    }

    String jfrFile = args[1];
    String memory = null;
    String cpu = null;
    String framework = "other";

    int i = 2;
    while (i < args.length) {
        String flag = args[i];
        switch (flag) {
            case "--container-memory" -> {
                if (i + 1 >= args.length) {
                    System.err.println("Error: --container-memory requires a value");
                    System.err.print(usage());
                    System.exit(1);
                }
                memory = args[++i];
            }
            case "--container-cpu" -> {
                if (i + 1 >= args.length) {
                    System.err.println("Error: --container-cpu requires a value");
                    System.err.print(usage());
                    System.exit(1);
                }
                cpu = args[++i];
            }
            case "--framework" -> {
                if (i + 1 >= args.length) {
                    System.err.println("Error: --framework requires a value");
                    System.err.print(usage());
                    System.exit(1);
                }
                framework = args[++i];
            }
            default -> {
                System.err.println("Error: unknown flag '" + flag + "'");
                System.err.print(usage());
                System.exit(1);
            }
        }
        i++;
    }

    Config config = new Config(jfrFile, memory, cpu, framework);
    System.out.println("Configuration received:");
    System.out.println("  jfr-file:         " + config.jfrFile());
    System.out.println("  container-memory: " + (config.memory() == null ? "(unset)" : config.memory()));
    System.out.println("  container-cpu:    " + (config.cpu() == null ? "(unset)" : config.cpu()));
    System.out.println("  framework:        " + config.framework());
}

void runDebugTool(String[] args) {
    if (args.length < 3) {
        System.err.println("Error: 'debug-tool' requires <tool-name> and tool args");
        System.err.println("Usage: jfrdoc debug-tool jfr-summary <jfr-file>");
        System.exit(1);
    }
    String toolName = args[1];
    switch (toolName) {
        case "jfr-summary" -> {
            String jfrPath = args[2];
            var tool = new JfrSummaryTool();
            var input = new JSONObject().put("path", jfrPath);
            String result = tool.execute(input);
            System.out.println(result);
            if (result.startsWith("Error:")) System.exit(1);
        }
        default -> {
            System.err.println("Error: unknown tool '" + toolName + "'");
            System.err.println("Available debug tools: jfr-summary");
            System.exit(1);
        }
    }
}

String usage() {
    return """
            jfrdoc - AI-powered JFR analyzer

            Usage:
              jfrdoc                       Show this help
              jfrdoc --help | -h           Show this help
              jfrdoc analyze <jfr-file> [flags]

            Flags (for analyze):
              --container-memory <value>   Container memory limit (e.g. 2Gi, 512Mi)
              --container-cpu <value>      Container CPU limit (e.g. 1, 500m)
              --framework <name>           One of: spring, quarkus, other (default: other)

            Examples:
              jfrdoc analyze recording.jfr
              jfrdoc analyze recording.jfr --container-memory 2Gi --framework quarkus
            """;
}
