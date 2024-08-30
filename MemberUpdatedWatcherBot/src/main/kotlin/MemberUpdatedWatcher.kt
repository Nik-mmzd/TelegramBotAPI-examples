import dev.inmo.tgbotapi.extensions.api.*
import dev.inmo.tgbotapi.extensions.api.bot.*
import dev.inmo.tgbotapi.extensions.api.send.*
import dev.inmo.tgbotapi.extensions.behaviour_builder.*
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.*
import dev.inmo.tgbotapi.extensions.behaviour_builder.utils.*
import dev.inmo.tgbotapi.extensions.utils.*
import dev.inmo.tgbotapi.types.chat.member.*
import dev.inmo.tgbotapi.utils.*


@OptIn(PreviewFeature::class)
suspend fun main(args: Array<String>) {
    val token = args.first()

    val bot = telegramBot(token)

    bot.buildBehaviourWithLongPolling {
        val me = getMe()
        val filterSelfUpdates = SimpleFilter<ChatMemberUpdated> {
            it.newChatMemberState.user.id == me.id
        }

        onChatMemberJoined(initialFilter = filterSelfUpdates) {
            println("Bot was added to chat")
            sendMessage(it.chat.id, "I was added to chat. Please grant me admin permissions to make me able to watch other users' events")
        }

        onChatMemberGotPromoted(initialFilter = filterSelfUpdates) {
            println("Bot was granted admin permissions")
            sendMessage(it.chat.id, "I was promoted to admin. I now can watch other users' events")
        }

        onChatMemberGotDemoted(initialFilter = filterSelfUpdates) {
            println("Admin permissions were revoked")
            sendMessage(it.chat.id, "I'm no longer an admin. Admin permissions are required to watch other users' events")
        }

        onChatMemberJoined {
            val member = it.newChatMemberState.user
            println("${member.firstName} joined the chat: ${it.oldChatMemberState::class.simpleName} => ${it.newChatMemberState::class.simpleName}")
            sendMessage(it.chat.id, "Welcome ${member.firstName}")
        }

        onChatMemberLeft {
            val member = it.newChatMemberState.user
            println("${member.firstName} left the chat: ${it.oldChatMemberState::class.simpleName} => ${it.newChatMemberState::class.simpleName}")
            sendMessage(it.chat.id, "Goodbye ${member.firstName}")
        }

        onChatMemberGotPromoted {
            val newState = it.newChatMemberState.requireAdministratorChatMember()
            println("${newState.user.firstName} got promoted to ${newState.customTitle ?: "Admin"}: ${it.oldChatMemberState::class.simpleName} => ${it.newChatMemberState::class.simpleName}")
            sendMessage(it.chat.id, "${newState.user.firstName} is now an ${newState.customTitle ?: "Admin"}")
        }

        onChatMemberGotDemoted {
            val member = it.newChatMemberState.user
            println("${member.firstName} got demoted: ${it.oldChatMemberState::class.simpleName} => ${it.newChatMemberState::class.simpleName}")
            sendMessage(it.chat.id, "${member.firstName} is now got demoted back to member")
        }

        onChatMemberGotPromotionChanged {
            val member = it.newChatMemberState.requireAdministratorChatMember()
            println("${member.user.firstName} has the permissions changed: ${it.oldChatMemberState::class.simpleName} => ${it.newChatMemberState::class.simpleName}")
        }
    }.join()
}