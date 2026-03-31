package org.springaicommunity.bench.core.result;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;

class AgentJournalTest {

	private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

	@Test
	void hasCostData_trueWhenTotalCostPositive() {
		AgentJournal journal = new AgentJournal("bench.journal.v1", List.of(), 5, 1000, 500, 0.05, 3000);
		assertThat(journal.hasCostData()).isTrue();
	}

	@Test
	void hasCostData_trueWhenPhaseCostPositive() {
		AgentJournal.Phase phase = new AgentJournal.Phase("plan", 2, 500, 200, 0.02, 1500, Map.of(), null);
		AgentJournal journal = new AgentJournal("bench.journal.v1", List.of(phase), 2, 500, 200, 0.0, 1500);
		assertThat(journal.hasCostData()).isTrue();
	}

	@Test
	void hasCostData_falseWhenNoCost() {
		AgentJournal journal = new AgentJournal("bench.journal.v1", List.of(), 3, 1000, 500, 0.0, 2000);
		assertThat(journal.hasCostData()).isFalse();
	}

	@Test
	void nullPhases_becomeEmptyList() {
		AgentJournal journal = new AgentJournal("bench.journal.v1", null, 0, 0, 0, 0.0, 0);
		assertThat(journal.phases()).isEmpty();
	}

	@Test
	void phase_nullToolUses_becomeEmptyMap() {
		AgentJournal.Phase phase = new AgentJournal.Phase("code", 3, 100, 50, 0.01, 500, null, null);
		assertThat(phase.toolUses()).isEmpty();
	}

	@Test
	void parseFromYaml() throws Exception {
		String yaml = """
				schema: bench.journal.v1
				totalTurns: 8
				totalInputTokens: 4000
				totalOutputTokens: 2000
				totalCostUsd: 0.12
				durationMs: 15000
				phases:
				  - name: plan
				    turns: 3
				    inputTokens: 1500
				    outputTokens: 800
				    costUsd: 0.05
				    durationMs: 6000
				    toolUses:
				      read: 5
				      write: 2
				  - name: code
				    turns: 5
				    inputTokens: 2500
				    outputTokens: 1200
				    costUsd: 0.07
				    durationMs: 9000
				""";
		AgentJournal journal = yamlMapper.readValue(yaml, AgentJournal.class);

		assertThat(journal.schema()).isEqualTo("bench.journal.v1");
		assertThat(journal.totalTurns()).isEqualTo(8);
		assertThat(journal.totalCostUsd()).isEqualTo(0.12);
		assertThat(journal.phases()).hasSize(2);
		assertThat(journal.phases().get(0).name()).isEqualTo("plan");
		assertThat(journal.phases().get(0).toolUses()).containsEntry("read", 5);
		assertThat(journal.hasCostData()).isTrue();
	}

	@Test
	void yamlDeserialization_ignoresUnknownFields() throws Exception {
		String yaml = """
				schema: bench.journal.v1
				totalTurns: 1
				totalInputTokens: 100
				totalOutputTokens: 50
				totalCostUsd: 0.0
				durationMs: 500
				futureField: something
				""";
		AgentJournal journal = yamlMapper.readValue(yaml, AgentJournal.class);
		assertThat(journal.totalTurns()).isEqualTo(1);
	}

}
