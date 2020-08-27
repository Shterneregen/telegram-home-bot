package random.telegramnetworkclientnotifier;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class CommandRunner {

	public List<String> runCommand(String command, String encoding) {
		List<String> result = new ArrayList<>();
		try {
			Process process = Runtime.getRuntime().exec(command);
			BufferedReader stdInput = new BufferedReader(
					new InputStreamReader(process.getInputStream(), encoding));
			BufferedReader stdError = new BufferedReader(
					new InputStreamReader(process.getErrorStream(), encoding));

			String s;
			while ((s = stdInput.readLine()) != null) {
				result.add(s);
			}

			while ((s = stdError.readLine()) != null) {
				result.add(s);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return result;
	}
}
