import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { Command } from 'app/shared/markdown-editor/commands/command';
import { BoldCommand } from 'app/shared/markdown-editor/commands/bold.command';
import { ItalicCommand } from 'app/shared/markdown-editor/commands/italic.command';
import { ReferenceCommand } from 'app/shared/markdown-editor/commands/reference.command';
import { UnderlineCommand } from 'app/shared/markdown-editor/commands/underline.command';
import { CodeBlockCommand } from 'app/shared/markdown-editor/commands/codeblock.command';
import { CodeCommand } from 'app/shared/markdown-editor/commands/code.command';

@Component({
    selector: 'jhi-postings-markdown-editor',
    templateUrl: './postings-markdown-editor.component.html',
    styleUrls: ['../postings.scss'],
})
export class PostingsMarkdownEditorComponent implements OnInit {
    @Input() text: String;
    @Output() textChange: EventEmitter<String> = new EventEmitter<string>();
    defaultCommands: Command[];

    constructor() {}

    ngOnInit(): void {
        this.defaultCommands = [new BoldCommand(), new ItalicCommand(), new UnderlineCommand(), new ReferenceCommand(), new CodeCommand(), new CodeBlockCommand()];
    }

    markdownChange(value: string) {
        this.text = value;
        this.textChange.emit(value);
    }
}
