package org.springaicommunity.bench.core.spec;

import java.util.HashMap;
import java.util.Map;

public record AgentSpec(String kind, // "claude-code", "goose", "openai-codex"
		String model, Boolean autoApprove, String prompt, Map<String, Object> genParams, String role // optional;
																										// default
																										// "coder"
) {

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String kind;

		private String model;

		private Boolean autoApprove;

		private String prompt;

		private Map<String, Object> genParams;

		private String role;

		public Builder kind(String kind) {
			this.kind = kind;
			return this;
		}

		public Builder model(String model) {
			this.model = model;
			return this;
		}

		public Builder autoApprove(Boolean autoApprove) {
			this.autoApprove = autoApprove;
			return this;
		}

		public Builder prompt(String prompt) {
			this.prompt = prompt;
			return this;
		}

		public Builder genParams(Map<String, Object> genParams) {
			this.genParams = genParams;
			return this;
		}

		public Builder genParam(String key, Object value) {
			if (this.genParams == null) {
				this.genParams = new HashMap<>();
			}
			this.genParams.put(key, value);
			return this;
		}

		public Builder role(String role) {
			this.role = role;
			return this;
		}

		public AgentSpec build() {
			return new AgentSpec(kind, model, autoApprove, prompt, genParams, role);
		}

	}
}
