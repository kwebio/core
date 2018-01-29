package io.kweb.demos.todo

import io.kweb.*
import io.kweb.demos.todo.State.Item
import io.kweb.demos.todo.State.List
import io.kweb.dom.element.*
import io.kweb.dom.element.creation.ElementCreator
import io.kweb.dom.element.creation.tags.*
import io.kweb.dom.element.creation.tags.InputType.text
import io.kweb.dom.element.events.on
import io.kweb.plugins.semanticUI.*
import io.kweb.routing.*
import io.kweb.state.Bindable
import io.kweb.state.persistent.*
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.future.await
import mu.KotlinLogging
import java.time.Instant

private val logger = KotlinLogging.logger {}

fun main(args: Array<String>) {
    // Starts a web server listening on port 8091
    Kweb(port = 8091, plugins = listOf(semanticUIPlugin)) {
        doc.body.new {
            route(withGalimatiasUrlParser) { url ->
                val path = url.path()
                h1().text("Todo List!")
                render(path[0]) { entityType ->
                    when (entityType) {
                        "" -> {
                            val newListId = generateNewUid()
                            State.lists[newListId] = State.List(newListId, "")
                            path.value = listOf("lists", newListId)
                        }
                        "lists" -> {
                            render(path[1]) { listUid ->
                                bind(asBindable(State.lists, listUid))
                            }
                        }
                        else -> {
                            throw NotFoundException("Unrecognized entity type '$entityType', path: ${path.value}")
                        }
                    }
                }
            }
        }
    }
    Thread.sleep(10000)
}

private fun ElementCreator<*>.bind(list : Bindable<State.List>) {
    logger.info("Rendering list ${list.value.uid}")
    h3().text(list.map(List::title))
    div(semantic.ui.middle.aligned.divided.list).new {
        renderEach(State.itemsByList(list.value.uid)) { item ->
            div(semantic.item).new {
                div(semantic.right.floated.content).new {
                    bindRemoveButton(item)
                }
                div(semantic.content).text(item.map(Item::text))
            }
        }
    }
    div(semantic.ui.action.input).new {
        val input = input(text, placeholder = "Add Item")
        input.on.keypress { ke ->
            if (ke.code == "13") {
                handleAddItem(input, list)
            }
        }
        button(semantic.ui.button).text("Add").on.click {
            handleAddItem(input, list)
        }
    }
}

private fun handleAddItem(input: InputElement, list: Bindable<List>) {
    async {
        val newItemText = input.getValue().await()
        input.setValue("")
        val newItem = Item(generateNewUid(), Instant.now(), list.value.uid, newItemText)
        State.items[newItem.uid] = newItem
    }
}

private fun ElementCreator<DivElement>.bindRemoveButton(item: Bindable<Item>): Element {
    return div(semantic.ui.button).text("Remove").on.click {
        State.items.remove(item.value.uid)
    }
}

private fun generateNewUid() = random.nextInt(100_000_000).toString(16)