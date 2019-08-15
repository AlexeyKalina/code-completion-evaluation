import * as vscode from 'vscode';
import * as fs from 'fs';
import * as url from "url";
import { Action, OpenFile, MoveCaret, PrintText, DeleteRange, CallCompletion, ActionsInfo } from './actions';
import { Session, Lookup, Suggestion, FileSessions } from './session';
import { SessionsEvaluationInfo, EvaluationInfo, CompletionStrategy } from './evaluationInfo';
import { performance } from 'perf_hooks';

export function activate(context: vscode.ExtensionContext) {
	let disposable = vscode.commands.registerCommand('extension.evaluateCompletion', async () => {

		let inputFile = await vscode.window.showOpenDialog({ 
			openLabel: 'Open Actions File', canSelectFolders: false, canSelectMany: false, filters: { 'Config': ['json']} 
		});
		let result = null;
		if (inputFile && inputFile.length > 0) {
			let file = fs.readFileSync(inputFile[0].path).toString('utf-8');
			let actionsInfo: ActionsInfo = JSON.parse(file);
			if (vscode.workspace.rootPath !== actionsInfo.projectPath) {
				vscode.window.showInformationMessage('Open project: ', actionsInfo.projectPath);
				return;
			}
			result = await interpretActions(actionsInfo.actions);
		}

		if (result !== null) {
			let outputDir = await vscode.window.showOpenDialog({ 
				openLabel: 'Select Output Dir', canSelectFiles: false, canSelectFolders: true, canSelectMany: false 
			});
			if (outputDir && outputDir.length === 1) {
				const path = new url.URL([outputDir[0].toString(), 'VS_CODE.json'].join('/'));
				fs.writeFileSync(path, JSON.stringify(result), { flag: 'w' });
				vscode.window.showInformationMessage('Evaluation completed.');
				return;
			}
		} else {
			vscode.window.showErrorMessage('Error during actions interpretation.');
		}
	});

	context.subscriptions.push(disposable);
}

async function interpretActions(actions: Action[]): Promise<SessionsEvaluationInfo> {
	await vscode.extensions.getExtension('vscode.java')!.activate();
	await vscode.extensions.getExtension('VisualStudioExptTeam.vscodeintellicode')!.activate();
	const wait_first_open = 5000;
	let editor: vscode.TextEditor | undefined;
	let sessions: Array<Session> | null = null;
	let files = new Array<FileSessions>();
	let curFile: FileSessions | null = null;
	let curSession: Session | null = null;
	let curPosition = 0;
	for (let action of actions) {
		editor = vscode.window.activeTextEditor;
		switch (action.type) {
			case 'OPEN_FILE':
				let openFile = action as OpenFile;
				if (curFile !== null && sessions !== null) {
					curFile.results = sessions;
					files.push(curFile);
				}
				await getDocument(openFile.path);
				if (curFile === null) {
					await delay(wait_first_open);
				}
				curFile = new FileSessions(openFile.path, openFile.text);
				sessions = null;
				break;
			case 'MOVE_CARET':
				let moveCaret = action as MoveCaret;
				let newPosition = editor!.document.positionAt(moveCaret.offset);
				const newSelection = new vscode.Selection(newPosition, newPosition);
				editor!.selection = newSelection;		
				curPosition = moveCaret.offset;
				break;
			case 'CALL_COMPLETION':
				if (curSession !== null && curSession.success) {
					continue;
				}
				let callCompletion = action as CallCompletion;
				if (curSession === null) {
					curSession = new Session(curPosition, callCompletion.expectedText, callCompletion.tokenType);
				}
				let time = performance.now();
				let completionResults = await vscode.commands.executeCommand<vscode.CompletionList>('vscode.executeCompletionItemProvider', editor!.document.uri, editor!.selection.active);
				let latency = Math.round(performance.now() - time);
				let suggestions = (completionResults as vscode.CompletionList).items.map(item => {
					return new Suggestion(item.insertText instanceof (vscode.SnippetString) ? item.insertText.value : item.insertText as string, item.label);
				});
				let lookup = new Lookup(callCompletion.prefix, suggestions, latency);
				curSession._lookups.push(lookup);
				curSession.success = suggestions.some(s => s.text === callCompletion.expectedText);
				break;
			case 'FINISH_SESSION':
				if (sessions === null) {
					sessions = new Array<Session>();
				}
				sessions.push(curSession!);
				curSession = null;
				break;
			case 'PRINT_TEXT':
				let printText = action as PrintText;
				await editor!.edit(builder => builder.insert(editor!.selection.active, printText.text));
				break;
			case 'DELETE_RANGE':
				let deleteRange = action as DeleteRange;
				await editor!.edit(builder => builder.delete(new vscode.Range(
					editor!.document.positionAt(deleteRange.begin), 
					editor!.document.positionAt(deleteRange.end))));
				break;
		}
	}
	if (curFile !== null && sessions !== null) {
		curFile.results = sessions;
		files.push(curFile);
	}
	return new SessionsEvaluationInfo(files, new EvaluationInfo("VS_CODE", new CompletionStrategy()));
}

async function getDocument(path: string) {
	let document = await vscode.workspace.openTextDocument(path);
	await vscode.window.showTextDocument(document, { preview: false });
	return document;
  }

function delay(ms: number) {
    return new Promise( resolve => setTimeout(resolve, ms) );
}
