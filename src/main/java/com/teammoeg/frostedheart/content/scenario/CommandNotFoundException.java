package com.teammoeg.frostedheart.content.scenario;

public class CommandNotFoundException extends ScenarioExecutionException {

	public CommandNotFoundException() {
	}

	public CommandNotFoundException(String message) {
		super(message);
	}

	public CommandNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public CommandNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public CommandNotFoundException(Throwable cause) {
		super(cause);
	}

}
