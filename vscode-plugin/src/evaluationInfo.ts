import { FileSessions } from "./session";

export class SessionsEvaluationInfo {
    constructor(public sessions: FileSessions[], public info: EvaluationInfo) {}
}

export class EvaluationInfo {
    constructor(public evaluationType: string, public strategy: CompletionStrategy) {}
}

export class CompletionStrategy {
}