terraform {
  required_providers {
    ovh = {
      source  = "ovh/ovh"
      version = "~> 2.7"
    }
    openstack = {
      source  = "terraform-provider-openstack/openstack"
      version = "~> 3.4"
    }
  }
}

provider "ovh" {
  endpoint = "ovh-eu"
}

provider "openstack" {
  auth_url    = "https://auth.cloud.ovh.net/v3"
  domain_name = "default"
  user_name   = var.openstack_user_name
  password    = var.openstack_password
  tenant_id   = var.service_name
  region      = "AF-NORTH-LZ-RBA-A"
}
/*Ce que j'ai trouvé : contrairement à instance.tf (qui utilise le provider ovh qu'on a déjà configuré), la gestion des Security Groups sur OVH passe par la couche OpenStack sous-jacente — les composants OpenStack sont le fondement de l'infrastructure Public Cloud d'OVHcloud, et Terraform utilise ces ressources via un second provider dédié, terraform-provider-openstack, séparé du provider ovh. Ce n'est pas une astuce de contournement, c'est le chemin documenté officiellement par OVH eux-mêmes pour ce type de ressource.

Ce que ça change concrètement : il faut un deuxième bloc provider dans provider.tf, avec des identifiants complètement différents de ceux qu'on a déjà — pas application_key/application_secret/consumer_key, mais un vrai couple nom d'utilisateur + mot de passe OpenStack, à créer séparément.*/


//Un point que je note pour plus tard, pas à résoudre maintenant : ce security group est créé via le provider openstack, alors que l'instance sera créée via le provider ovh — il faudra confirmer, une fois à instance.tf, comment lier concrètement les deux entre eux. Je te le signalerai à ce moment précis.