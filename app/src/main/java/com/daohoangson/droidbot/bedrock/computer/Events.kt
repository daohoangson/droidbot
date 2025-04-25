@file:OptIn(ExperimentalSerializationApi::class)

package com.daohoangson.droidbot.bedrock.computer

import com.daohoangson.droidbot.bedrock.message.Message
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

sealed class ComputerUseEvent() {
    data class Text(
        val text: String
    ) : ComputerUseEvent()

    data class ComputerUse(
        val id: String,
        val input: ComputerUseInput,
        val name: String,
    ) : ComputerUseEvent()

    data class MessageStopEvent(
        val message: Message
    ) : ComputerUseEvent()
}


@Serializable
@JsonClassDiscriminator("action")
sealed class ComputerUseInput() {
    @Serializable
    @SerialName("cursor_position")
    data class CursorPosition(val unused: Unit? = null) : ComputerUseInput()

    @Serializable
    @SerialName("double_click")
    data class DoubleClick(val unused: Unit? = null) : ComputerUseInput()

    @Serializable
    @SerialName("finish")
    data class Finish(val unused: Unit? = null) : ComputerUseInput()

    @Serializable
    @SerialName("key")
    data class Key(val text: String) : ComputerUseInput()

    @Serializable
    @SerialName("left_click")
    data class LeftClick(val unused: Unit? = null) : ComputerUseInput()

    @Serializable
    @SerialName("left_click_drag")
    data class LeftClickDrag(val coordinate: Pair<Int, Int>) : ComputerUseInput()

    @Serializable
    @SerialName("middle_click")
    data class MiddleClick(val unused: Unit? = null) : ComputerUseInput()

    @Serializable
    @SerialName("mouse_move")
    data class MouseMove(val coordinate: Pair<Int, Int>) : ComputerUseInput()

    @Serializable
    @SerialName("right_click")
    data class RightClick(val unused: Unit? = null) : ComputerUseInput()

    @Serializable
    @SerialName("screenshot")
    data class Screenshot(val unused: Unit? = null) : ComputerUseInput()

    @Serializable
    @SerialName("type")
    data class Type(val text: String) : ComputerUseInput()
}

