package org.springaicommunity.bench.core.cli;

import org.springaicommunity.agents.judge.Judge;
import org.springaicommunity.agents.judge.Judges;
import org.springaicommunity.agents.judge.fs.FileContentJudge;
import org.springaicommunity.agents.judge.fs.FileExistsJudge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for Judge instances. Each benchmark case gets its own Judge bean.
 * The bean name must match the case ID from the YAML files.
 */
@Configuration
public class JudgeConfiguration {

	/**
	 * Judge for the hello-world benchmark case. Verifies that hello.txt exists and
	 * contains exactly "Hello World!".
	 * @return composed judge using FileExistsJudge and FileContentJudge
	 */
	@Bean(name = "hello-world")
	public Judge helloWorldJudge() {
		return Judges.allOf(new FileExistsJudge("hello.txt"),
				new FileContentJudge("hello.txt", "Hello World!", FileContentJudge.MatchMode.EXACT));
	}

}
