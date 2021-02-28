package random.telegramhomebot.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import random.telegramhomebot.model.Host;
import random.telegramhomebot.repository.HostRepository;
import random.telegramhomebot.utils.pagination.PagerHelper;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Optional;
import java.util.UUID;

import static random.telegramhomebot.AppConstants.Hosts.ADD_EDIT_HOST_VIEW;
import static random.telegramhomebot.AppConstants.Hosts.DELETE_HOST_MAPPING;
import static random.telegramhomebot.AppConstants.Hosts.EDIT_HOST_BY_ID_MAPPING;
import static random.telegramhomebot.AppConstants.Hosts.EDIT_HOST_MAPPING;
import static random.telegramhomebot.AppConstants.Hosts.HOSTS_MAPPING;
import static random.telegramhomebot.AppConstants.Hosts.HOSTS_MODEL_ATTR;
import static random.telegramhomebot.AppConstants.Hosts.HOSTS_VIEW;
import static random.telegramhomebot.AppConstants.Hosts.HOST_ID_PATH_VAR;
import static random.telegramhomebot.AppConstants.Hosts.HOST_MAC_FIELD;
import static random.telegramhomebot.AppConstants.Hosts.HOST_MODEL_ATTR;
import static random.telegramhomebot.AppConstants.Hosts.REDIRECT_HOSTS;
import static random.telegramhomebot.AppConstants.Hosts.SAVE_HOST_MAPPING;
import static random.telegramhomebot.AppConstants.Messages.HOST_MAC_NOT_UNIQUE_MSG;

@Controller
@RequestMapping(HOSTS_MAPPING)
public class HostController {

	private static final int DEFAULT_CURRENT_PAGE = 0;

	@Resource
	private HostRepository hostRepository;

	@Value("${hosts.default.page.size}")
	private int defaultPageSize;
	@Value("${hosts.default.sorting}")
	private String defaultSorting;
	@Value("${hosts.default.sorting.direction}")
	private String defaultSortingDirection;

	@RequestMapping
	public String getAllHosts(@RequestParam("pageSize") Optional<Integer> pageSize,
							  @RequestParam("page") Optional<Integer> currentPage,
							  @RequestParam("sortBy") Optional<String> sortBy,
							  @RequestParam("direction") Optional<String> direction,
							  Model model) {

		PageRequest pageable = PagerHelper.getPageable(
				pageSize, defaultPageSize,
				currentPage, DEFAULT_CURRENT_PAGE,
				sortBy, defaultSorting,
				direction, defaultSortingDirection
		);

		Page<Host> hosts = hostRepository.findAll(pageable);
		model.addAttribute(HOSTS_MODEL_ATTR, hosts);

		PagerHelper.prepareModelForPager(
				model, pageable.getPageSize(), hosts.getTotalPages(), hosts.getNumber(), "hosts");
		return HOSTS_VIEW;
	}

	@RequestMapping(path = {EDIT_HOST_MAPPING, EDIT_HOST_BY_ID_MAPPING})
	public String editHostById(Model model, @PathVariable(HOST_ID_PATH_VAR) Optional<UUID> id) {
		model.addAttribute(HOST_MODEL_ATTR, Optional.ofNullable(id)
				.filter(Optional::isPresent)
				.flatMap(macOp -> hostRepository.findById(id.get()))
				.orElseGet(Host::new));
		return ADD_EDIT_HOST_VIEW;
	}

	@RequestMapping(path = DELETE_HOST_MAPPING)
	public String deleteHostById(@PathVariable(HOST_ID_PATH_VAR) UUID id) {
		hostRepository.deleteById(id);
		return REDIRECT_HOSTS;
	}

	@PostMapping(path = SAVE_HOST_MAPPING)
	public String createOrUpdateHost(@Valid Host host, BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			return ADD_EDIT_HOST_VIEW;
		}
		Optional<Host> storedHost = hostRepository.findHostByMac(host.getMac());
		boolean saveNewHostWithExistingMac = storedHost.isPresent() && host.getId() == null;
		boolean editStoredHostMacToExisting = storedHost.isPresent() && !storedHost.get().getId().equals(host.getId());
		if (saveNewHostWithExistingMac || editStoredHostMacToExisting) {
			bindingResult.rejectValue(HOST_MAC_FIELD, HOST_MAC_NOT_UNIQUE_MSG);
			return ADD_EDIT_HOST_VIEW;
		}
		hostRepository.save(host);
		return REDIRECT_HOSTS;
	}
}
