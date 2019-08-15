import { Guid } from "guid-typescript";

export class Session {
    constructor(public offset: number, public expectedText: string, public tokenType: string) {
    }
    _lookups: Lookup[] = [];
    id: string = Guid.create().toString();
    success: boolean = false;
}

export class Lookup {
    constructor(public text: string, public suggestions: Suggestion[], public latency: number) {
    }
}

export class Suggestion {
    constructor(public text: string, public presentationText: string) {
    }
}

export class FileSessions {
    constructor(public filePath: string, public text: string) {
    }
    results: Session[] = [];
}