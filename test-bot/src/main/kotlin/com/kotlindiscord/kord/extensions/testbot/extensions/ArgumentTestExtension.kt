/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.testbot.extensions

import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.attachment
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalAttachment
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalString
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.interactions.commands.Command

public class ArgumentTestExtension : Extension() {
    override val name: String = "test-args"

    override suspend fun setup() {
        publicSlashCommand(::OptionalArgs) {
            name = "optional-autocomplete"
            description = "Check whether autocomplete works with an optional converter."

            action {
                respond {
                    content = "You provided: `${arguments.response}`"
                }
            }
        }

        publicSlashCommand(::LengthConstrainedArgs) {
            name = "length-constrained"
            description = "Check if length limits work"

            action {
                respond {
                    content = buildString {
                        append("You name is: `${arguments.name}`")
                        arguments.lastName?.let {
                            append(" `$it`")
                        }
                    }
                }
            }
        }

        publicSlashCommand(::AttachmentArguments) {
            name = "attachment"
            description = "Check attachment command options."

            action {
                respond {
                    content = buildString {
                        append("You attached: ${arguments.file.fileName}.")

                        arguments.optionalFile?.let {
                            append("\nYou also attached: ${it.fileName}")
                        }
                    }
                }
            }
        }
    }

    public inner class OptionalArgs : Arguments() {
        public val response: String? by optionalString {
            name = "response"
            description = "Text to receive"

            autoComplete {
                this.replyChoices(
                    Command.Choice("one", "one"),
                    Command.Choice("two", "Two"),
                    Command.Choice("three", "Three"),
                )
            }
        }
    }

    public inner class LengthConstrainedArgs : Arguments() {
        public val name: String by string {
            name = "name"
            description = "The user's name."
            minLength = 3
            maxLength = 10
        }

        public val lastName: String? by optionalString {
            name = "last_name"
            description = "The user's last name."
            minLength = 4
            maxLength = 15
        }
    }

    public inner class AttachmentArguments : Arguments() {
        public val file: Message.Attachment by attachment {
            name = "file"
            description = "An attached file."
        }

        public val optionalFile: Message.Attachment? by optionalAttachment {
            name = "optional_file"
            description = "An optional file."
        }
    }
}
