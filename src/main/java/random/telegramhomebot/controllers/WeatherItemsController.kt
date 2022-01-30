package random.telegramhomebot.controllers

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.thymeleaf.spring5.context.webflux.ReactiveDataDriverContextVariable
import random.telegramhomebot.const.AppConstants
import random.telegramhomebot.openweather.db.WeatherItem
import random.telegramhomebot.openweather.db.WeatherItemRepository
import reactor.core.publisher.Mono
import java.util.UUID

@ConditionalOnProperty(prefix = "openweather", value = ["enabled"], havingValue = "true")
@Controller
@RequestMapping("/weather")
class WeatherItemsController(private val repository: WeatherItemRepository) {

    @RequestMapping
    fun getAllWeatherItems(model: Model): String {
        model.addAttribute("weatherItems", ReactiveDataDriverContextVariable(repository.findAll()))
        return "weatherItems"
    }

    @RequestMapping(path = ["/edit", "/edit/{id}"])
    fun editById(model: Model, @PathVariable("id") id: UUID?): Mono<String> {
//        val weatherItem =
//            if (id == null) WeatherItem()
//            else id.let { repository.findById(it).orElse(null) } ?: return AppConstants.ERROR_404_REDIRECT
//
//        model.addAttribute("weatherItem", weatherItem)
//        return "weatherItem"

        return (id?.let { repository.findById(it) } ?: Mono.just(WeatherItem()))
            .flatMap { weatherItem ->
                model.addAttribute("weatherItem", weatherItem)
                Mono.just(AppConstants.ADD_EDIT_COMMAND_VIEW)
            }.switchIfEmpty(Mono.just(AppConstants.ERROR_404_REDIRECT))
    }

    @RequestMapping(path = ["/delete/{id}"])
    fun deleteById(@PathVariable("id") id: UUID): String {
        repository.deleteById(id).subscribe()
        return "redirect:/weather"
    }

    @PostMapping(path = ["/save"])
    fun createOrUpdate(weatherItem: WeatherItem): String {
        repository.save(weatherItem).subscribe()
        return "redirect:/weather"
    }
}
