package com.quickdelivery.services.interfaces;

import com.quickdelivery.abstarct.dto.AddressBookEntryDTO;

import java.util.List;

public interface IAddressBookService {
    List<AddressBookEntryDTO> search(Long ownerUserId, String query, Integer limit);
    AddressBookEntryDTO save(Long ownerUserId, AddressBookEntryDTO request);
}
