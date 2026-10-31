package random.telegramhomebot.services.commands;

import java.nio.file.Files;
import java.nio.file.Path;

/** A real subprocess without a shell, credentials, networking or platform utilities. */
public final class CommandProcessFixture {
    public static void main(String[] args) throws Exception {
        switch (args[0]) {
            case "stderr":
                for (int i = 0; i < 65536; i++) System.err.print("stderr ");
                break;
            case "sleep":
                Files.writeString(Path.of(args[1]), Long.toString(ProcessHandle.current().pid()));
                Thread.sleep(60000);
                break;
            case "descendants":
            case "background":
                ProcessBuilder builder = new ProcessBuilder(
                        Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                        "-cp", System.getProperty("java.class.path"),
                        CommandProcessFixture.class.getName(), "sleep", args[1] + ".child");
                if (args[0].equals("background")) {
                    builder.redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD);
                } else {
                    builder.inheritIO();
                }
                Process child = builder.start();
                Files.writeString(Path.of(args[1]), ProcessHandle.current().pid() + "\n" + child.pid());
                if (args[0].equals("background")) {
                    Thread.sleep(300);
                    System.out.print("started");
                } else {
                    child.waitFor();
                }
                break;
            default:
                System.out.print("ok");
        }
    }
}
