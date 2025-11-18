package org.springframework.samples.petclinic.customer.service;

import org.springframework.samples.petclinic.customer.db.OwnerRepository;
import org.springframework.samples.petclinic.customer.db.PetRepository;
import org.springframework.samples.petclinic.customer.model.Owner;
import org.springframework.samples.petclinic.customer.model.Pet;
import org.springframework.samples.petclinic.customer.model.PetType;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
public class CustomerService {

    private final OwnerRepository owners;
    private final PetRepository pets;

    public CustomerService(OwnerRepository owners, PetRepository pets) {
        this.owners = owners;
        this.pets = pets;
    }

    public Collection<Owner> ownerByLastName(String lastName) {
        return owners.findByLastName(lastName);
    }

    public Owner ownerById(int ownerId) {
        return owners.findById(ownerId);
    }

    public void save(Owner owner) {
        owners.save(owner);
    }

    // 기존 메서드명도 유지 (호환성)
    public Collection<Owner> findOwnersByLastName(String lastName) {
        return ownerByLastName(lastName);
    }

    public Owner findOwnerById(int ownerId) {
        return ownerById(ownerId);
    }

    public void saveOwner(Owner owner) {
        save(owner);
    }

    public Pet findPetById(int petId) {
        return pets.findById(petId);
    }
    
    public Pet petById(int petId) {
        return pets.findById(petId);
    }

    public List<PetType> findPetTypes() {
        return pets.findPetTypes();
    }
    
    public Collection<PetType> petTypes() {
        return pets.findPetTypes();
    }

    public void savePet(Pet pet) {
        pets.save(pet);
    }
    
    public void save(Pet pet) {
        pets.save(pet);
    }

    // TODO: API Gateway 단계에서 Feature Flag를 통해 Visit 서비스 연동 예정
    // 현재는 임시로 빈 리스트 반환 (Visit 서비스 분리 후 수정)
    public List<?> visitsByPetId(int petId) {
        // Feature Flag로 모놀리스 Visit API 호출하도록 변경 예정
        return new java.util.ArrayList<>();
    }
}