export interface Action {
	type: string;
}

export class ActionsInfo{
	constructor(public projectPath: string, public actions: Action[]) {}
}

export class MoveCaret implements Action {
	type: string = 'MOVE_CARET';
	offset!: number;
}

export class OpenFile implements Action {
	type: string = 'OPEN_FILE';
	path!: string;
	text!: string;
}

export class PrintText implements Action {
	type: string = 'PRINT_TEXT';
	text!: string;
	completable!: boolean;
}

export class DeleteRange implements Action {
	type: string = 'DELETE_RANGE';
	begin!: number;
	end!: number;
	completable!: boolean;
}

export class CallCompletion implements Action {
	type: string = 'CALL_COMPLETION';
	prefix!: string;
	expectedText!: string;
	tokenType!: string;
}

export class FinishSession implements Action {
	type: string = "FINISH_SESSION";
}